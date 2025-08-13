package com.hxg.queue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hxg.model.dto.CatalogueStruct;
import com.hxg.model.entity.Catalogue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author hxg
 * @description: 文档生成任务模型
 * @date 2025/8/5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentGenerationTask {
    /**
     * 任务唯一ID
     */
    private String taskId;
    
    /**
     * 目录ID
     */
    private String catalogueId;
    
    /**
     * 目录名称
     */
    private String catalogueName;
    
    /**
     * 生成提示词
     */
    private String prompt;
    
    /**
     * 项目本地路径
     */
    private String localPath;
    
    /**
     * 文件树结构
     */
    private String fileTree;
    
    /**
     * 目录结构
     */
    private CatalogueStruct catalogueStruct;
    
    /**
     * 重试次数
     */
    private Integer retryCount = 0;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 任务优先级
     */
    private String priority = "NORMAL";
    
    /**
     * 项目名称（用于Memory服务索引）
     */
    private String projectName;
    
    /**
     * 创建文档生成任务
     * @param catalogue 目录实体
     * @param fileTree 文件树
     * @param catalogueStruct 目录结构
     * @param localPath 本地路径
     * @return 文档生成任务
     */
    public static DocumentGenerationTask create(Catalogue catalogue, String fileTree, 
                                              CatalogueStruct catalogueStruct, String localPath) {
        DocumentGenerationTask task = new DocumentGenerationTask();
        task.setTaskId(catalogue.getTaskId());
        task.setCatalogueId(catalogue.getCatalogueId());
        task.setCatalogueName(catalogue.getName());
        task.setPrompt(catalogue.getPrompt());
        task.setLocalPath(localPath);
        task.setFileTree(fileTree);
        task.setCatalogueStruct(catalogueStruct);
        task.setRetryCount(0);
        task.setCreateTime(LocalDateTime.now());
        task.setPriority("NORMAL");
        // projectName将在CatalogueServiceImpl中设置
        return task;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    /**
     * 是否超过最大重试次数
     */
    public boolean exceedsMaxRetries(int maxRetries) {
        return this.retryCount > maxRetries;
    }
}