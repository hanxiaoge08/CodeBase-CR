package com.hxg.crApp.client;

import com.hxg.crApp.dto.CodeReviewContextRequest;
import com.hxg.crApp.dto.ContentTypeSearchRequest;
import com.hxg.crApp.dto.MemorySearchResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Memory服务Feign客户端 - Review模块专用
 * 用于Review模块调用Memory模块的代码评审记忆服务
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
     * 为代码评审搜索相关上下文
     */
    @PostMapping("/api/memory/code-review/context")
    String searchContextForCodeReview(@RequestBody CodeReviewContextRequest request);
    
    /**
     * 按内容类型搜索
     */
    @PostMapping("/api/memory/code-review/search")
    MemorySearchResultDto.SearchResponse searchByContentType(@RequestBody ContentTypeSearchRequest request);
    
    /**
     * 搜索相关文档
     */
    @PostMapping("/api/memory/code-review/search/documents")
    MemorySearchResultDto.SearchResponse searchRelatedDocuments(@RequestBody ContentTypeSearchRequest request);
    
    /**
     * 搜索相关代码文件
     */
    @PostMapping("/api/memory/code-review/search/code-files")
    MemorySearchResultDto.SearchResponse searchRelatedCodeFiles(@RequestBody ContentTypeSearchRequest request);
    
    /**
     * 健康检查
     */
    @GetMapping("/api/memory/health")
    String healthCheck();
}