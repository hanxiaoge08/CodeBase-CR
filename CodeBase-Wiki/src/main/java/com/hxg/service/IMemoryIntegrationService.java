package com.hxg.service;

import com.hxg.model.entity.Catalogue;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 记忆集成服务接口
 * 负责将Wiki生成的内容存储到Mem0记忆系统
 * 
 * @author AI Assistant
 */
public interface IMemoryIntegrationService {
    
    /**
     * 异步索引项目的文档和代码内容到Mem0
     * 
     * @param repositoryId 仓库标识（应该是projectName）
     * @param catalogues 生成的文档目录列表
     * @param projectPath 项目路径
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> indexProjectToMemoryAsync(String repositoryId, 
                                                      List<Catalogue> catalogues, 
                                                      String projectPath);
    
    /**
     * 异步索引单个文档到Mem0
     * 
     * @param repositoryId 仓库标识（应该是projectName）
     * @param catalogue 文档目录
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> indexDocumentToMemoryAsync(String repositoryId, Catalogue catalogue);
    
    /**
     * 异步索引项目代码文件到Mem0
     * 
     * @param repositoryId 仓库标识（应该是projectName）
     * @param projectPath 项目路径
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> indexCodeFilesToMemoryAsync(String repositoryId, String projectPath);
    
    /**
     * 检查Mem0服务是否可用
     * 
     * @return boolean
     */
    boolean isMemoryServiceAvailable();
} 