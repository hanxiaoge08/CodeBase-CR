package com.hxg.client;

import com.hxg.dto.BatchDocumentRequest;
import com.hxg.dto.CodeFileRequest;
import com.hxg.dto.DocumentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Memory服务Feign客户端
 * 用于Wiki模块调用Memory模块的记忆服务
 * 
 * @author AI Assistant
 */
@FeignClient(
    name = "memory-service", 
    url = "${memory.service.url:http://localhost:8080}",
    fallback = MemoryServiceClientFallback.class
)
public interface MemoryServiceClient {
    
    /**
     * 添加单个文档到记忆
     */
    @PostMapping("/api/memory/documents")
    void addDocument(@RequestBody DocumentRequest request);
    
    /**
     * 批量添加文档到记忆
     */
    @PostMapping("/api/memory/documents/batch")
    void batchAddDocuments(@RequestBody BatchDocumentRequest request);
    
    /**
     * 添加代码文件到记忆
     */
    @PostMapping("/api/memory/code-files")
    void addCodeFile(@RequestBody CodeFileRequest request);
    
    /**
     * 健康检查
     */
    @GetMapping("/api/memory/health")
    String healthCheck();
}