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
import com.hxg.utils.RegexUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.hxg.model.vo.CatalogueListVo;

/**
 * @author hxg
 * @description: 目录服务实现类
 * @date 2025/7/22 23:35
 */
@Slf4j
@Service
public class CatalogueServiceImpl extends ServiceImpl<CatalogueMapper, Catalogue> implements ICatalogueService {
    private final LlmService llmService;
    private final IMemoryIntegrationService memoryIntegrationService;

    public CatalogueServiceImpl(LlmService llmService, IMemoryIntegrationService memoryIntegrationService) {
        this.llmService = llmService;
        this.memoryIntegrationService = memoryIntegrationService;
    }

    @Override
    public GenCatalogueDTO generateCatalogue(String fileTree, ExecutionContext context) {
        //生成项目目录
        String genCataloguePrompt= AnalyzeCataloguePrompt.prompt
                .replace("{{$code_files}}",fileTree)
                .replace("{{$repository_location}}",context.getLocalPath());
        log.info("LLM开始生成项目目录，prompt内容：{}",genCataloguePrompt);
        String result=llmService.callWithTools(genCataloguePrompt);
        log.info("LLM生成项目目录结果：{}",result);
        
        //String documentationStructure= RegexUtil.extractXmlTagContent(result,"<documentation_structure>","</documentation_structure>");
        CatalogueStruct catalogueStruct = processCatalogueStruct(result);
        List<Catalogue> catalogueList = saveCatalogueStruct(context, catalogueStruct);
        return new GenCatalogueDTO(catalogueStruct,catalogueList);
    }

    @Override
    public CatalogueStruct processCatalogueStruct(String result) {
        String documentationStructure=result;
        if(result.startsWith("<documentation_structure>")){
            documentationStructure = RegexUtil.extractXmlTagContent(result,"<documentation_structure>","</documentation_structure>");
        }
        
        try{
            CatalogueStruct catalogueStruct=JSON.parseObject(documentationStructure, CatalogueStruct.class);
            if (catalogueStruct == null || catalogueStruct.getItems().isEmpty()) {
                log.error("LLM生成项目目录结构为空");
                throw new Exception("LLM生成项目目录结构为空");
            }
            log.info("LLM生成项目目录结构内容：{}",documentationStructure);
            return catalogueStruct;
        }catch (Exception e){
            throw new RuntimeException("LLM生成项目目录结构为空");
        }
        
    }

    @Override
    public List<Catalogue> saveCatalogueStruct(ExecutionContext context, CatalogueStruct catalogueStruct) {
        List<Catalogue> catalogueList = catalogueStruct.getItems().stream().map(catalogue -> {
            Catalogue catalogueEntity = new Catalogue();
            catalogueEntity.setTaskId(context.getTask().getTaskId());
            catalogueEntity.setCatalogueId(java.util.UUID.randomUUID().toString());
            catalogueEntity.setParentCatalogueId(null);//TODO: 根据实际结构设置
            catalogueEntity.setName(catalogue.getName());
            catalogueEntity.setTitle(catalogue.getTitle());
            catalogueEntity.setPrompt(catalogue.getPrompt());
            catalogueEntity.setDependentFile(JSON.toJSONString(catalogue.getDependent_file()));
            catalogueEntity.setChildren(JSON.toJSONString(catalogue.getChildren()));
            catalogueEntity.setStatus(CatalogueStatusEnum.IN_PROGRESS.getCode());
            catalogueEntity.setCreateTime(LocalDateTime.now());
            return catalogueEntity;
        }).collect(Collectors.toList());
        
        this.saveBatch(catalogueList);
        return catalogueList;
    }

    @Override
    public void parallelGenerateCatalogueDetail(String fileTree, GenCatalogueDTO genCatalogueDTO, String localPath) {
        genCatalogueDTO.getCatalogueList().forEach(catalogue -> {
            if(StringUtils.hasText(catalogue.getParentCatalogueId())){
                generateCatalogueDetail(catalogue,fileTree,genCatalogueDTO.getCatalogueStruct(),localPath);
            }
        });
        
        // 文档生成完成后，异步索引到Mem0记忆系统
        indexCataloguesToMemoryAsync(genCatalogueDTO, localPath);
    }

    @Override
    @Async("GenCatalogueDetailExecutor")
    public void generateCatalogueDetail(Catalogue catalogue, String fileTree, CatalogueStruct catalogueStruct, String localPath) {
        try{
            log.info("LLM开始生成目录详情：{}", catalogue.getName());
            String prompt= GenDocPrompt.prompt
                    .replace("{{repository_location}}",localPath)
                    .replace("{{prompt}}",catalogue.getPrompt())
                    .replace("{{title}}",catalogue.getName())
                    .replace("{{$repository_files}}",fileTree)
                    .replace("{{$catalogue}}",JSON.toJSONString(catalogueStruct));
            String result=llmService.callWithTools(prompt);
            log.info("LLM生成{}目录详情结果：{}",catalogue.getName(),result);
            if(StringUtils.isEmpty(result)){
                throw new RuntimeException("LLM生成目录详情结果为空");
            }
            //保存目录详情
            catalogue.setContent(result);
            catalogue.setStatus(CatalogueStatusEnum.COMPLETED.getCode());
        }catch (Exception e){
            log.error("LLM生成{}目录详情失败",catalogue.getName(),e);
            catalogue.setStatus(CatalogueStatusEnum.FAILED.getCode());
            catalogue.setFailReason(e.getMessage());
        }finally {
            this.updateById(catalogue);
            
            // 单个文档生成完成后，尝试索引到Mem0
            if (catalogue.getStatus() != null && 
                catalogue.getStatus().equals(CatalogueStatusEnum.COMPLETED.getCode()) &&
                StringUtils.hasText(catalogue.getContent())) {
                
                indexSingleCatalogueToMemoryAsync(catalogue);
            }
        }
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
     * 异步索引目录列表到Mem0记忆系统
     */
    private void indexCataloguesToMemoryAsync(GenCatalogueDTO genCatalogueDTO, String localPath) {
        if (memoryIntegrationService.isMemoryServiceAvailable()) {
            String taskId = genCatalogueDTO.getCatalogueList().get(0).getTaskId();
            
            log.info("开始异步索引目录到Mem0记忆系统: taskId={}", taskId);
            
            memoryIntegrationService.indexProjectToMemoryAsync(
                taskId,
                genCatalogueDTO.getCatalogueList(),
                localPath
            ).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("索引目录到Mem0失败: taskId={}", taskId, throwable);
                } else {
                    log.info("索引目录到Mem0完成: taskId={}", taskId);
                }
            });
        } else {
            log.debug("Mem0记忆服务不可用，跳过索引");
        }
    }
    
    /**
     * 异步索引单个目录到Mem0记忆系统
     */
    private void indexSingleCatalogueToMemoryAsync(Catalogue catalogue) {
        if (memoryIntegrationService.isMemoryServiceAvailable()) {
            log.debug("异步索引单个目录到Mem0: catalogueName={}", catalogue.getName());
            
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
        }
    }
}
