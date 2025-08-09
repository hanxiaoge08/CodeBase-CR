package com.hxg.queue.service;

import com.alibaba.fastjson2.JSON;
import com.hxg.llm.prompt.GenDocPrompt;
import com.hxg.llm.service.LlmService;
import com.hxg.llm.tool.FileSystemTool;
import com.hxg.model.mapper.CatalogueMapper;
import com.hxg.model.mapper.TaskMapper;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.entity.Task;
import com.hxg.model.enums.CatalogueStatusEnum;
import com.hxg.queue.model.DocumentGenerationTask;
import com.hxg.service.IMemoryIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * @author hxg
 * @description: 文档处理服务
 * @date 2025/8/5
 */
@Service
@Slf4j
public class DocumentProcessingService {
    
    private final LlmService llmService;
    private final CatalogueMapper catalogueMapper;
    private final TaskMapper taskMapper;
    private final IMemoryIntegrationService memoryIntegrationService;
    
    @Value("${project.wiki.prompt.doc-version}")
    private String docPromptVersion;
    
    public DocumentProcessingService(LlmService llmService, 
                                   CatalogueMapper catalogueMapper,
                                   TaskMapper taskMapper,
                                   IMemoryIntegrationService memoryIntegrationService) {
        this.llmService = llmService;
        this.catalogueMapper = catalogueMapper;
        this.taskMapper = taskMapper;
        this.memoryIntegrationService = memoryIntegrationService;
        log.info("DocumentProcessingService initialized with docPromptVersion: {}", docPromptVersion);
    }
    
    /**
     * 处理文档生成任务
     * @param task 文档生成任务
     */
    @Transactional
    public void processTask(DocumentGenerationTask task) {
        String taskId = task.getTaskId();
        String catalogueName = task.getCatalogueName();
        
        log.info("开始处理文档生成任务: taskId={}, catalogueName={}, retryCount={}", 
                taskId, catalogueName, task.getRetryCount());
        
        // 1. 首先检查任务是否还存在
        Task existingTask = getTaskById(taskId);
        if (existingTask == null) {
            log.warn("任务已被删除，跳过处理: taskId={}, catalogueName={}", taskId, catalogueName);
            throw new TaskDeletedException("任务已被删除: " + taskId);
        }
        
        // 2. 检查目录记录是否存在
        Catalogue existingCatalogue = catalogueMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Catalogue>()
                .eq(Catalogue::getCatalogueId, task.getCatalogueId())
        );
        
        if (existingCatalogue == null) {
            log.warn("目录记录已被删除，跳过处理: taskId={}, catalogueId={}", taskId, task.getCatalogueId());
            throw new TaskDeletedException("目录记录已被删除: " + task.getCatalogueId());
        }
        
        // 先清理可能存在的旧ThreadLocal值
        FileSystemTool.clearProjectRoot();
        
        // 设置项目根路径到 ThreadLocal，供 FileSystemTool 使用
        FileSystemTool.setProjectRoot(task.getLocalPath());
        log.debug("为任务 {} 设置项目根路径: {}", taskId, task.getLocalPath());
        
        try {
            // 获取对应版本的prompt模板
            String prompt = getPromptByVersion(docPromptVersion);
            if (prompt == null) {
                throw new RuntimeException("未找到对应版本的prompt模板: " + docPromptVersion);
            }
            
            // 构建完整的prompt
            prompt = buildPrompt(prompt, task);
            
            log.info("开始生成目录详情，使用prompt版本: {}, catalogueName: {}, promptLength: {}", 
                    docPromptVersion, catalogueName, prompt.length());
            
            // 调用LLM服务生成内容
            String result = llmService.callWithTools(prompt);
            
            if (!StringUtils.hasText(result)) {
                throw new RuntimeException("LLM生成目录详情结果为空");
            }
            
            log.info("LLM生成完成: taskId={}, catalogueName={}, resultLength={}", 
                    taskId, catalogueName, result.length());
            
            // 更新数据库状态为完成
            updateCatalogueStatus(task.getCatalogueId(), result, 
                    CatalogueStatusEnum.COMPLETED.getCode(), null);
            
            // 异步索引到Mem0记忆系统
            indexToMemorySystemAsync(task, result);
            
            log.info("文档生成任务处理完成: taskId={}, catalogueName={}", taskId, catalogueName);
            
        } catch (TaskDeletedException e) {
            // 任务已删除异常，直接重新抛出，不需要更新状态
            throw e;
        } catch (Exception e) {
            log.error("处理文档生成任务失败: taskId={}, catalogueName={}, error={}", 
                    taskId, catalogueName, e.getMessage(), e);
            
            // 更新数据库状态为失败
            updateCatalogueStatus(task.getCatalogueId(), null, 
                    CatalogueStatusEnum.FAILED.getCode(), e.getMessage());
            
            // 重新抛出异常，让消费者处理重试逻辑
            throw new RuntimeException("文档生成失败: " + e.getMessage(), e);
        } finally {
            // 确保清理 ThreadLocal，避免内存泄漏和状态污染
            try {
                FileSystemTool.clearProjectRoot();
                log.debug("任务 {} 完成，已清理ThreadLocal", taskId);
            } catch (Exception cleanupError) {
                log.warn("清理ThreadLocal时发生异常: taskId={}, error={}", taskId, cleanupError.getMessage());
            }
        }
    }
    
    /**
     * 根据版本获取prompt模板
     */
    private String getPromptByVersion(String version) {
        return switch (version) {
            case "v1" -> GenDocPrompt.promptV1;
            case "v2" -> GenDocPrompt.promptV2;
            case "v3" -> GenDocPrompt.promptV3;
            default -> {
                log.warn("未知的prompt版本: {}, 使用默认版本v3", version);
                yield GenDocPrompt.promptV3;
            }
        };
    }
    
    /**
     * 构建完整的prompt
     */
    private String buildPrompt(String template, DocumentGenerationTask task) {
        return template
                .replace("{{repository_location}}", task.getLocalPath())
                .replace("{{prompt}}", task.getPrompt())
                .replace("{{title}}", task.getCatalogueName())
                .replace("{{repository_files}}", task.getFileTree())
                .replace("{{catalogue}}", JSON.toJSONString(task.getCatalogueStruct()));
    }
    
    /**
     * 更新目录状态
     */
    private void updateCatalogueStatus(String catalogueId, String content, Integer status, String failReason) {
        try {
            // 查询现有记录
            Catalogue existingCatalogue = catalogueMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Catalogue>()
                    .eq(Catalogue::getCatalogueId, catalogueId)
            );
            
            if (existingCatalogue == null) {
                log.error("无法找到要更新的目录记录: catalogueId={}", catalogueId);
                return;
            }
            
            // 更新记录内容和状态
            existingCatalogue.setContent(content);
            existingCatalogue.setStatus(status);
            existingCatalogue.setFailReason(failReason);
            existingCatalogue.setUpdateTime(LocalDateTime.now());
            
            int updated = catalogueMapper.updateById(existingCatalogue);
            if (updated > 0) {
                log.info("目录状态更新成功: catalogueId={}, status={}", catalogueId, status);
            } else {
                log.warn("目录状态更新失败: catalogueId={}, status={}", catalogueId, status);
            }
            
        } catch (Exception e) {
            log.error("更新目录状态失败: catalogueId={}, status={}, error={}", 
                    catalogueId, status, e.getMessage(), e);
        }
    }
    
    /**
     * 异步索引到Mem0记忆系统
     */
    private void indexToMemorySystemAsync(DocumentGenerationTask task, String content) {
        if (!memoryIntegrationService.isMemoryServiceAvailable()) {
            log.debug("Mem0记忆服务不可用，跳过索引: taskId={}", task.getTaskId());
            return;
        }
        
        try {
            // 构建Catalogue对象用于索引
            Catalogue catalogue = new Catalogue();
            catalogue.setTaskId(task.getTaskId());
            catalogue.setCatalogueId(task.getCatalogueId());
            catalogue.setName(task.getCatalogueName());
            catalogue.setContent(content);
            catalogue.setStatus(CatalogueStatusEnum.COMPLETED.getCode());
            
            // 异步索引单个文档
            memoryIntegrationService.indexDocumentToMemoryAsync(task.getTaskId(), catalogue)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("索引文档到Mem0失败: taskId={}, catalogueName={}, error={}", 
                                task.getTaskId(), task.getCatalogueName(), throwable.getMessage());
                    } else {
                        log.info("文档索引到Mem0成功: taskId={}, catalogueName={}", 
                                task.getTaskId(), task.getCatalogueName());
                    }
                });
            
            // 触发代码文件索引（每个任务只执行一次）
            memoryIntegrationService.indexCodeFilesToMemoryAsync(task.getTaskId(), task.getLocalPath())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("索引代码文件到Mem0失败: taskId={}, error={}", 
                                task.getTaskId(), throwable.getMessage());
                    } else {
                        log.info("代码文件索引到Mem0成功: taskId={}", task.getTaskId());
                    }
                });
                
        } catch (Exception e) {
            log.warn("索引到Mem0记忆系统时发生异常: taskId={}, error={}", 
                    task.getTaskId(), e.getMessage(), e);
        }
    }
    
    /**
     * 根据taskId查询任务
     */
    private Task getTaskById(String taskId) {
        try {
            return taskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Task>()
                    .eq(Task::getTaskId, taskId)
            );
        } catch (Exception e) {
            log.error("查询任务失败: taskId={}, error={}", taskId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 任务已删除异常
     */
    public static class TaskDeletedException extends RuntimeException {
        public TaskDeletedException(String message) {
            super(message);
        }
    }
}