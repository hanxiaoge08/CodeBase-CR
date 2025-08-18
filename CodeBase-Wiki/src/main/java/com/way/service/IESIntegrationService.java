package com.way.service;

import com.way.model.entity.Catalogue;

import java.util.concurrent.CompletableFuture;

/**
 * ES集成服务接口
 * 负责将Wiki生成的内容存储到ES
 * 
 * @author AI Assistant
 */
public interface IESIntegrationService {
    /**
     * 异步索引项目代码文件到ES
     *
     * @param repositoryId 仓库标识（应该是projectName）
     * @param taskId 任务ID
     * @param projectPath 项目路径
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> indexCodeFilesToESAsync(String repositoryId, String taskId, String projectPath);

    /**
     * 异步索引单个文档到ES
     * 
     * @param repositoryId 仓库标识（应该是projectName）
     * @param catalogue 文档目录
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> indexDocumentToESAsync(String repositoryId, Catalogue catalogue);

}