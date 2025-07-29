package com.hxg.client;

import com.hxg.model.dto.BatchDocumentRequest;
import com.hxg.model.dto.CodeFileRequest;
import com.hxg.model.dto.DocumentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Memory服务Feign客户端熔断降级处理
 * 
 * @author AI Assistant
 */
@Component
public class MemoryServiceClientFallback implements MemoryServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryServiceClientFallback.class);
    
    @Override
    public void addDocument(DocumentRequest request) {
        logger.warn("Memory服务不可用，跳过文档记忆索引: repositoryId={}, documentName={}", 
            request.getRepositoryId(), request.getDocumentName());
    }
    
    @Override
    public void batchAddDocuments(BatchDocumentRequest request) {
        logger.warn("Memory服务不可用，跳过批量文档记忆索引: repositoryId={}, documentCount={}", 
            request.getRepositoryId(), request.getDocuments() != null ? request.getDocuments().size() : 0);
    }
    
    @Override
    public void addCodeFile(CodeFileRequest request) {
        logger.warn("Memory服务不可用，跳过代码文件记忆索引: repositoryId={}, fileName={}", 
            request.getRepositoryId(), request.getFileName());
    }
    
    @Override
    public String healthCheck() {
        logger.warn("Memory服务健康检查失败，服务不可用");
        return "Memory Service Unavailable";
    }
}