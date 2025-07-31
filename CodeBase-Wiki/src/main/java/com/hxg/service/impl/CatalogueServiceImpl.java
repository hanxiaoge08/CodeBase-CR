package com.hxg.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxg.context.ExecutionContext;
import com.hxg.llm.prompt.GenDocPrompt;
import com.hxg.llm.service.LlmService;
import com.hxg.llm.prompt.AnalyzeCataloguePrompt;
import com.hxg.mapper.CatalogueMapper;
import com.hxg.model.dto.CatalogueStruct;
import com.hxg.model.dto.GenCatalogueDTO;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.enums.CatalogueStatusEnum;
import com.hxg.service.ICatalogueService;
import com.hxg.service.IMemoryIntegrationService;
import com.hxg.service.async.CatalogueDetailAsyncService;
import com.hxg.utils.RegexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.hxg.model.vo.CatalogueListVo;
import org.springframework.util.StringUtils;

/**
 * @author hxg
 * @description: 目录服务实现类
 * @date 2025/7/22 23:35
 */
@Slf4j
@Service
public class CatalogueServiceImpl extends ServiceImpl<CatalogueMapper, Catalogue> implements ICatalogueService {
    @Value("${project.wiki.prompt.catalogue-version}")
    private String cataloguePromptVersion;
    
    private final LlmService llmService;
    private final IMemoryIntegrationService memoryIntegrationService;
    private final CatalogueDetailAsyncService catalogueDetailAsyncService;

    public CatalogueServiceImpl(LlmService llmService, 
                              IMemoryIntegrationService memoryIntegrationService,
                              CatalogueDetailAsyncService catalogueDetailAsyncService) {
        this.llmService = llmService;
        this.memoryIntegrationService = memoryIntegrationService;
        this.catalogueDetailAsyncService = catalogueDetailAsyncService;
    }

    @Override
    public GenCatalogueDTO generateCatalogue(String fileTree, ExecutionContext context) {
        String genCataloguePrompt = switch (cataloguePromptVersion) {
            case "v1" -> AnalyzeCataloguePrompt.promptV1;
            case "v2" -> AnalyzeCataloguePrompt.promptV2;
            case "v3" -> AnalyzeCataloguePrompt.promptV3;
            default -> AnalyzeCataloguePrompt.promptV3;
        };
        genCataloguePrompt = genCataloguePrompt
                .replace("{{$code_files}}", fileTree)
                .replace("{{$repository_location}}", context.getLocalPath());
        log.info("LLM开始生成项目目录，使用prompt版本: {}", cataloguePromptVersion);
        String result=llmService.callWithTools(genCataloguePrompt);
        log.info("LLM生成项目目录完成");
        
        CatalogueStruct catalogueStruct = processCatalogueStruct(result);
        List<Catalogue> catalogueList = saveCatalogueStruct(context, catalogueStruct);
        return new GenCatalogueDTO(catalogueStruct,catalogueList);
    }

    @Override
    public CatalogueStruct processCatalogueStruct(String result) {
        String documentationStructure = result;
        
        // 处理XML标签格式的返回
        if(result.startsWith("<documentation_structure>")){
            documentationStructure = RegexUtil.extractXmlTagContent(result,"<documentation_structure>","</documentation_structure>");
        }
        
        try{
            // 首先尝试直接解析为CatalogueStruct
            CatalogueStruct catalogueStruct = JSON.parseObject(documentationStructure, CatalogueStruct.class);
            
            // 如果直接解析失败或items为空，尝试从documentation_structure字段中提取
            if (catalogueStruct == null || catalogueStruct.getItems() == null || catalogueStruct.getItems().isEmpty()) {
                log.debug("直接解析失败，尝试从documentation_structure字段提取");
                
                // 尝试解析包含documentation_structure字段的JSON
                com.alibaba.fastjson2.JSONObject jsonObject = JSON.parseObject(documentationStructure);
                if (jsonObject.containsKey("documentation_structure")) {
                    catalogueStruct = jsonObject.getObject("documentation_structure", CatalogueStruct.class);
                }
            }
            
            if (catalogueStruct == null || catalogueStruct.getItems() == null || catalogueStruct.getItems().isEmpty()) {
                log.error("LLM生成项目目录结构为空或无效，原始内容：{}", documentationStructure);
                throw new RuntimeException("LLM生成项目目录结构为空或无效");
            }
            
            log.info("LLM生成项目目录结构解析成功，items数量：{}", catalogueStruct.getItems().size());
            return catalogueStruct;
            
        } catch (Exception e) {
            log.error("解析LLM生成的目录结构时发生错误，原始内容：{}", documentationStructure, e);
            throw new RuntimeException("解析LLM生成的目录结构失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Catalogue> saveCatalogueStruct(ExecutionContext context, CatalogueStruct catalogueStruct) {
        List<Catalogue> allCatalogueList = new ArrayList<>();
        
        // 递归处理所有目录节点
        for (CatalogueStruct.Item item : catalogueStruct.getItems()) {
            allCatalogueList.addAll(saveCatalogueItem(context, item, null));
        }
        
        log.info("保存目录结构完成，总共保存了{}个目录节点", allCatalogueList.size());
        return allCatalogueList;
    }
    
    /**
     * 递归保存目录项及其子项
     * @param context 执行上下文
     * @param item 目录项
     * @param parentCatalogueId 父目录ID
     * @return 保存的目录列表
     */
    private List<Catalogue> saveCatalogueItem(ExecutionContext context, CatalogueStruct.Item item, String parentCatalogueId) {
        List<Catalogue> catalogueList = new ArrayList<>();
        
        // 创建当前目录实体
        Catalogue catalogueEntity = new Catalogue();
        catalogueEntity.setTaskId(context.getTask().getTaskId());
        catalogueEntity.setCatalogueId(java.util.UUID.randomUUID().toString());
        catalogueEntity.setParentCatalogueId(parentCatalogueId);
        catalogueEntity.setName(item.getName());
        catalogueEntity.setTitle(item.getTitle());
        catalogueEntity.setPrompt(item.getPrompt());
        catalogueEntity.setDependentFile(JSON.toJSONString(item.getDependent_file()));
        
        // 子目录信息也保存，但主要用于前端显示结构
        catalogueEntity.setChildren(JSON.toJSONString(item.getChildren()));
        catalogueEntity.setStatus(CatalogueStatusEnum.IN_PROGRESS.getCode());
        catalogueEntity.setCreateTime(LocalDateTime.now());
        
        // 保存当前目录
        this.save(catalogueEntity);
        catalogueList.add(catalogueEntity);
        
        log.debug("保存目录节点: name={}, catalogueId={}, parentId={}", 
                item.getName(), catalogueEntity.getCatalogueId(), parentCatalogueId);
        
        // 递归处理子目录
        if (item.getChildren() != null && !item.getChildren().isEmpty()) {
            for (CatalogueStruct.Item child : item.getChildren()) {
                catalogueList.addAll(saveCatalogueItem(context, child, catalogueEntity.getCatalogueId()));
            }
        }
        
        return catalogueList;
    }

    @Override
    public void parallelGenerateCatalogueDetail(String fileTree, GenCatalogueDTO genCatalogueDTO, String localPath) {
        // 过滤出需要生成详细内容的目录
        List<Catalogue> cataloguesToProcess = genCatalogueDTO.getCatalogueList().stream()
                .filter(catalogue -> catalogue != null && StringUtils.hasText(catalogue.getName()))
                .collect(Collectors.toList());
                
        log.info("开始并行生成目录详情，总数={}", cataloguesToProcess.size());
        
        for (Catalogue catalogue : cataloguesToProcess) {
            // 缓存项目路径以避免循环依赖
            catalogueDetailAsyncService.cacheTaskProjectPath(catalogue.getTaskId(), localPath);
            
            // 为每个目录创建专门的上下文信息
            CatalogueStruct specificContext = createSpecificContext(catalogue, genCatalogueDTO.getCatalogueStruct());
            
            // 调用独立的异步服务，传递特定的上下文
            catalogueDetailAsyncService.generateCatalogueDetail(catalogue, fileTree, specificContext, localPath);
        }
        
        log.info("所有目录详情生成任务已启动");
    }
    
    /**
     * 为特定目录创建专门的上下文信息
     * @param targetCatalogue 目标目录
     * @param fullStruct 完整的目录结构
     * @return 针对该目录的上下文信息
     */
    private CatalogueStruct createSpecificContext(Catalogue targetCatalogue, CatalogueStruct fullStruct) {
        CatalogueStruct specificContext = new CatalogueStruct();
        
        // 查找目标目录在结构中的位置和上下文
        CatalogueStruct.Item targetItem = findTargetItem(targetCatalogue, fullStruct.getItems());
        
        if (targetItem != null) {
            // 创建包含目标项及其上下文的结构
            List<CatalogueStruct.Item> contextItems = new ArrayList<>();
            contextItems.add(targetItem);
            specificContext.setItems(contextItems);
            
            log.debug("为目录 {} 创建特定上下文，包含 {} 个依赖文件", 
                    targetCatalogue.getName(), 
                    targetItem.getDependent_file() != null ? targetItem.getDependent_file().size() : 0);
        } else {
            // 如果找不到目标项，使用完整结构作为fallback
            log.warn("无法为目录 {} 找到对应的结构项，使用完整结构", targetCatalogue.getName());
            specificContext = fullStruct;
        }
        
        return specificContext;
    }
    
    /**
     * 在目录树中查找目标目录项
     */
    private CatalogueStruct.Item findTargetItem(Catalogue targetCatalogue, List<CatalogueStruct.Item> items) {
        if (items == null) return null;
        
        for (CatalogueStruct.Item item : items) {
            // 匹配名称和标题
            if ((targetCatalogue.getName() != null && targetCatalogue.getName().equals(item.getName())) ||
                (targetCatalogue.getTitle() != null && targetCatalogue.getTitle().equals(item.getTitle()))) {
                return item;
            }
            
            // 递归查找子项
            CatalogueStruct.Item found = findTargetItem(targetCatalogue, item.getChildren());
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }

    @Override
    public void deleteCatalogueByTaskId(String taskId) {
        this.lambdaUpdate()
                .eq(Catalogue::getTaskId, taskId)
                .remove();
    }

    @Override
    public List<Catalogue> getCatalogueByTaskId(String taskId) {
        return this.lambdaQuery()
                .eq(Catalogue::getTaskId, taskId)
                .list();
    }

    /**
     * 根据taskId获取目录树形结构
     */
    @Override
    public List<CatalogueListVo> getCatalogueTreeByTaskId(String taskId) {
        List<Catalogue> catalogues = getCatalogueByTaskId(taskId);
        if (catalogues.isEmpty()) {
            return List.of();
        }

        // 找到根节点（没有parentCatalogueId的节点）
        List<CatalogueListVo> rootNodes = catalogues.stream()
                .filter(catalogue -> !StringUtils.hasText(catalogue.getParentCatalogueId()))
                .map(this::convertToCatalogueListVo)
                .collect(Collectors.toList());

        // 为每个根节点构建子树
        rootNodes.forEach(rootNode -> buildCatalogueTree(rootNode, catalogues));

        return rootNodes;
    }

    /**
     * 构建目录树形结构
     */
    private void buildCatalogueTree(CatalogueListVo parentNode, List<Catalogue> allCatalogues) {
        List<CatalogueListVo> children = allCatalogues.stream()
                .filter(catalogue -> parentNode.getCatalogueId().equals(catalogue.getParentCatalogueId()))
                .map(this::convertToCatalogueListVo)
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            parentNode.setChildren(children);
            // 递归构建子节点的子树
            children.forEach(child -> buildCatalogueTree(child, allCatalogues));
        }
    }

    /**
     * 将Catalogue实体转换为CatalogueListVo
     */
    private CatalogueListVo convertToCatalogueListVo(Catalogue catalogue) {
        CatalogueListVo vo = new CatalogueListVo();
        vo.setCatalogueId(catalogue.getCatalogueId());
        vo.setParentCatalogueId(catalogue.getParentCatalogueId());
        vo.setName(catalogue.getName());
        vo.setTitle(catalogue.getTitle());
        vo.setPrompt(catalogue.getPrompt());
        vo.setDependentFile(catalogue.getDependentFile());
        vo.setContent(catalogue.getContent());
        vo.setStatus(catalogue.getStatus());
        return vo;
    }
    
    /**
     * 异步索引单个目录到Mem0记忆系统
     */
    private void indexSingleCatalogueToMemoryAsync(Catalogue catalogue) {
        if (memoryIntegrationService.isMemoryServiceAvailable()) {
            log.debug("异步索引单个目录到Mem0: catalogueName={}", catalogue.getName());
            
            // 索引单个文档
            memoryIntegrationService.indexDocumentToMemoryAsync(
                catalogue.getTaskId(),
                catalogue
            ).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.warn("索引单个目录到Mem0失败: catalogueName={}", catalogue.getName(), throwable);
                } else {
                    log.debug("索引单个目录到Mem0完成: catalogueName={}", catalogue.getName());
                }
            });
            
            // 检查是否是第一个完成的文档，如果是则触发代码文件索引
            // 使用synchronized确保只有一个线程能执行代码文件索引
            triggerCodeFileIndexingOnce(catalogue);
        }
    }
    
    // 用于确保每个任务的代码文件只被索引一次的标记
    private final Set<String> codeFilesIndexedTasks = ConcurrentHashMap.newKeySet();
    
    // 存储任务ID和项目路径的映射，避免循环依赖
    private final Map<String, String> taskProjectPaths = new ConcurrentHashMap<>();
    
    /**
     * 触发代码文件索引（每个任务只执行一次）
     */
    private void triggerCodeFileIndexingOnce(Catalogue catalogue) {
        String taskId = catalogue.getTaskId();
        if (codeFilesIndexedTasks.add(taskId)) {
            log.info("触发代码文件索引: taskId={}", taskId);
            
            // 从缓存中获取项目路径
            String projectPath = taskProjectPaths.get(taskId);
            if (projectPath != null) {
                memoryIntegrationService.indexCodeFilesToMemoryAsync(
                    taskId, 
                    projectPath
                ).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("索引代码文件到Mem0失败: taskId={}", taskId, throwable);
                        // 如果失败，移除标记以便重试
                        codeFilesIndexedTasks.remove(taskId);
                    } else {
                        log.info("索引代码文件到Mem0完成: taskId={}", taskId);
                        // 成功后清理路径缓存
                        taskProjectPaths.remove(taskId);
                    }
                });
            } else {
                log.warn("无法获取项目路径进行代码文件索引: taskId={}", taskId);
                // 如果无法获取路径，移除标记
                codeFilesIndexedTasks.remove(taskId);
            }
        }
    }
    
    /**
     * 缓存任务的项目路径，避免循环依赖
     */
    public void cacheTaskProjectPath(String taskId, String projectPath) {
        taskProjectPaths.put(taskId, projectPath);
        log.debug("缓存任务项目路径: taskId={}, path={}", taskId, projectPath);
    }
    
    /**
     * 清理任务相关的缓存数据
     */
    public void cleanupTaskCache(String taskId) {
        taskProjectPaths.remove(taskId);
        codeFilesIndexedTasks.remove(taskId);
        log.debug("清理任务缓存: taskId={}", taskId);
    }
    
}
