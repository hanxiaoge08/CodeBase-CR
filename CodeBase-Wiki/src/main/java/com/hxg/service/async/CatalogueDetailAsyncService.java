package com.hxg.service.async;

import com.alibaba.fastjson2.JSON;
import com.hxg.llm.prompt.GenDocPrompt;
import com.hxg.llm.service.LlmService;
import com.hxg.model.dto.CatalogueStruct;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.enums.CatalogueStatusEnum;
import com.hxg.mapper.CatalogueMapper;
import com.hxg.service.IMemoryIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hxg
 * @description: 目录详情异步生成服务
 * @date 2025/7/30
 */
@Slf4j
@Service
public class CatalogueDetailAsyncService {
    
    private final LlmService llmService;
    private final CatalogueMapper catalogueMapper;
    private final IMemoryIntegrationService memoryIntegrationService;
    
    // 用于确保每个任务的代码文件只被索引一次的标记
    private final Set<String> codeFilesIndexedTasks = ConcurrentHashMap.newKeySet();
    
    // 存储任务ID和项目路径的映射，避免循环依赖
    private final Map<String, String> taskProjectPaths = new ConcurrentHashMap<>();
    
    public CatalogueDetailAsyncService(LlmService llmService, 
                                     CatalogueMapper catalogueMapper,
                                     IMemoryIntegrationService memoryIntegrationService) {
        this.llmService = llmService;
        this.catalogueMapper = catalogueMapper;
        this.memoryIntegrationService = memoryIntegrationService;
    }
    
    /**
     * 异步生成目录详情
     */
    @Async("GenCatalogueDetailExecutor")
    @Transactional
    public void generateCatalogueDetail(Catalogue catalogue, String fileTree, CatalogueStruct catalogueStruct, String localPath) {
        String taskId = catalogue.getTaskId();
        String catalogueName = catalogue.getName();
        
        log.info("异步生成目录详情：taskId={}, catalogueName={}", taskId, catalogueName);
        
        try{
            String prompt= GenDocPrompt.prompt
                    .replace("{{repository_location}}",localPath)
                    .replace("{{prompt}}",catalogue.getPrompt())
                    .replace("{{title}}",catalogueName)
                    .replace("{{$repository_files}}",fileTree)
                    .replace("{{$catalogue}}",JSON.toJSONString(catalogueStruct));
                    
            String result=llmService.callWithTools(prompt);
            
            if(StringUtils.isEmpty(result)){
                throw new RuntimeException("LLM生成目录详情结果为空");
            }
            
            catalogue.setContent(result);
            catalogue.setStatus(CatalogueStatusEnum.COMPLETED.getCode());
            
        }catch (Exception e){
            log.error("LLM生成目录详情失败：taskId={}, catalogueName={}, error={}", 
                    taskId, catalogueName, e.getMessage(), e);
            catalogue.setStatus(CatalogueStatusEnum.FAILED.getCode());
            catalogue.setFailReason(e.getMessage());
        }finally {
            try {
                // 先查询出完整的记录，获取主键ID
                Catalogue existingCatalogue = catalogueMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Catalogue>()
                        .eq(Catalogue::getCatalogueId, catalogue.getCatalogueId())
                );
                
                if (existingCatalogue == null) {
                    log.error("无法找到要更新的目录记录：catalogueId={}", catalogue.getCatalogueId());
                    return;
                }
                
                // 更新现有记录的内容和状态
                existingCatalogue.setContent(catalogue.getContent());
                existingCatalogue.setStatus(catalogue.getStatus());
                existingCatalogue.setFailReason(catalogue.getFailReason());
                existingCatalogue.setUpdateTime(LocalDateTime.now());
                        
                catalogueMapper.updateById(existingCatalogue);
                log.info("目录更新完成：taskId={}, catalogueName={}", taskId, catalogueName);
                
            } catch (Exception e) {
                log.error("更新目录状态失败：taskId={}, catalogueName={}", taskId, catalogueName, e);
            }
            
            // 索引到Mem0记忆系统
            if (catalogue.getStatus() != null && 
                catalogue.getStatus().equals(CatalogueStatusEnum.COMPLETED.getCode()) &&
                StringUtils.hasText(catalogue.getContent())) {
                indexSingleCatalogueToMemoryAsync(catalogue);
            }
        }
    }
    
    /**
     * 异步索引单个目录到Mem0记忆系统
     */
    private void indexSingleCatalogueToMemoryAsync(Catalogue catalogue) {
        if (memoryIntegrationService.isMemoryServiceAvailable()) {
            // 索引单个文档
            memoryIntegrationService.indexDocumentToMemoryAsync(
                catalogue.getTaskId(),
                catalogue
            ).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.warn("索引目录到Mem0失败: catalogueName={}", catalogue.getName(), throwable);
                }
            });
            
            // 触发代码文件索引
            triggerCodeFileIndexingOnce(catalogue);
        }
    }
    
    /**
     * 触发代码文件索引（每个任务只执行一次）
     */
    private void triggerCodeFileIndexingOnce(Catalogue catalogue) {
        String taskId = catalogue.getTaskId();
        if (codeFilesIndexedTasks.add(taskId)) {
            String projectPath = taskProjectPaths.get(taskId);
            if (projectPath != null) {
                memoryIntegrationService.indexCodeFilesToMemoryAsync(
                    taskId, 
                    projectPath
                ).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("索引代码文件到Mem0失败: taskId={}", taskId, throwable);
                        codeFilesIndexedTasks.remove(taskId);
                    } else {
                        taskProjectPaths.remove(taskId);
                    }
                });
            } else {
                codeFilesIndexedTasks.remove(taskId);
            }
        }
    }
    
    /**
     * 缓存任务的项目路径，避免循环依赖
     */
    public void cacheTaskProjectPath(String taskId, String projectPath) {
        taskProjectPaths.put(taskId, projectPath);
    }
    
    public void cleanupTaskCache(String taskId) {
        taskProjectPaths.remove(taskId);
        codeFilesIndexedTasks.remove(taskId);
    }
}