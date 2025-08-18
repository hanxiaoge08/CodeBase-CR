package com.way.crApp.client;

import com.way.crApp.dto.CodeReviewContextRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Wiki服务Feign客户端熔断降级处理 - Review模块专用
 * 当Wiki服务不可用时，提供降级响应
 * 
 * @author way
 */
@Component
public class ESWikiServiceClientFallback implements ESWikiServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ESWikiServiceClientFallback.class);

    @Override
    public String searchContextForCodeReview(CodeReviewContextRequest request) {
        logger.warn("Wiki服务不可用，使用降级逻辑处理代码评审上下文请求: repositoryId={}, prTitle={}",
                request.getRepositoryId(), request.getPrTitle());
        
        // 返回基本的上下文信息，帮助代码评审继续进行
        StringBuilder fallbackContext = new StringBuilder();
        fallbackContext.append("## 代码评审上下文 (降级模式)\n\n");
        fallbackContext.append("⚠️ Wiki检索服务暂时不可用，以下为基本信息：\n\n");
        
        if (request.getPrTitle() != null) {
            fallbackContext.append("**PR标题**: ").append(request.getPrTitle()).append("\n");
        }
        
        if (request.getPrDescription() != null) {
            fallbackContext.append("**PR描述**: ").append(request.getPrDescription()).append("\n");
        }
        
        if (request.getChangedFiles() != null && !request.getChangedFiles().isEmpty()) {
            fallbackContext.append("**变更文件**: \n");
            for (String file : request.getChangedFiles()) {
                fallbackContext.append("- ").append(file).append("\n");
            }
        }
        
        fallbackContext.append("\n**注意**: 由于检索服务不可用，缺少相关代码上下文，");
        fallbackContext.append("建议基于变更内容和通用最佳实践进行评审。\n");
        
        String result = fallbackContext.toString();
        logger.info("返回降级上下文，长度: {}字符", result.length());
        
        return result;
    }

    @Override
    public Map<String, Object> healthCheck() {
        logger.debug("Wiki服务健康检查降级响应");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "DEGRADED");
        response.put("service", "wiki-service");
        response.put("message", "Service unavailable, using fallback");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
}
