package com.way.crApp.client;

import com.way.crApp.dto.CodeReviewContextRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Wiki服务Feign客户端 - Review模块专用
 * 用于Review模块调用Wiki模块的ES检索服务
 * 替代原有的MemoryServiceClient，提供更强的混合检索能力
 *
 * @author way
 */
@FeignClient(
        name = "wiki-service",
        url = "${wiki.service.url:http://localhost:8085}",
        fallback = ESWikiServiceClientFallback.class
)
public interface ESWikiServiceClient {
    
    /**
     * 为代码评审搜索相关上下文
     * 使用增强混合检索(BM25 + kNN + RRF)提供高质量上下文
     * 
     * @param request 代码评审上下文请求
     * @return 格式化的上下文字符串
     */
    @PostMapping("/api/chat/reviewContext")
    String searchContextForCodeReview(@RequestBody CodeReviewContextRequest request);
    
    /**
     * Wiki服务健康检查
     * 
     * @return 健康状态信息
     */
    @GetMapping("/api/health")
    Map<String, Object> healthCheck();
}
