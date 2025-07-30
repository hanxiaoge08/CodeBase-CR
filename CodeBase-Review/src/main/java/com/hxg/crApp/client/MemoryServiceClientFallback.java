package com.hxg.crApp.client;

import com.hxg.crApp.dto.CodeReviewContextRequest;
import com.hxg.crApp.dto.ContentTypeSearchRequest;
import com.hxg.crApp.dto.MemorySearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memory服务Feign客户端熔断降级处理 - Review模块专用
 * 
 * @author AI Assistant
 */
@Component
public class MemoryServiceClientFallback implements MemoryServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryServiceClientFallback.class);
    
    @Override
    public String searchContextForCodeReview(CodeReviewContextRequest request) {
        logger.warn("Memory服务不可用，返回模拟的代码评审上下文: repositoryId={}", request.getRepositoryId());
        
        return buildMockContextResponse(request.getRepositoryId(), request.getPrTitle(), request.getDiffContent());
    }
    
    @Override
    public MemorySearchResultDto.SearchResponse searchByContentType(ContentTypeSearchRequest request) {
        logger.warn("Memory服务不可用，返回空搜索结果: repositoryId={}, contentType={}", 
            request.getRepositoryId(), request.getContentType());
        
        return new MemorySearchResultDto.SearchResponse(List.of(), 0);
    }
    
    @Override
    public MemorySearchResultDto.SearchResponse searchRelatedDocuments(ContentTypeSearchRequest request) {
        logger.warn("Memory服务不可用，无法搜索相关文档: repositoryId={}", request.getRepositoryId());
        
        return new MemorySearchResultDto.SearchResponse(List.of(), 0);
    }
    
    @Override
    public MemorySearchResultDto.SearchResponse searchRelatedCodeFiles(ContentTypeSearchRequest request) {
        logger.warn("Memory服务不可用，无法搜索相关代码文件: repositoryId={}", request.getRepositoryId());
        
        return new MemorySearchResultDto.SearchResponse(List.of(), 0);
    }
    
    @Override
    public Map<String, Object> healthCheck() {
        logger.warn("Memory服务健康检查失败，服务不可用");
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("status", "DOWN");
        fallbackResponse.put("message", "Memory Service Unavailable");
        return fallbackResponse;
    }
    
    /**
     * 构建模拟的上下文响应
     */
    private String buildMockContextResponse(String repositoryId, String prTitle, String diffContent) {
        StringBuilder context = new StringBuilder();
        context.append("=== 相关项目上下文信息 (模拟模式) ===\n\n");
        
        // 分析PR标题和内容
        if (prTitle != null && !prTitle.isEmpty()) {
            context.append("📋 **PR主题**: ").append(prTitle).append("\n\n");
        }
        
        // 分析代码变更
        if (diffContent != null && !diffContent.isEmpty()) {
            String[] lines = diffContent.split("\n");
            int addedLines = 0;
            int removedLines = 0;
            
            for (String line : lines) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    addedLines++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    removedLines++;
                }
            }
            
            context.append("📊 **变更统计**: ").append("+").append(addedLines)
                   .append(" -").append(removedLines).append(" 行\n\n");
        }
        
        // 模拟相关文档
        context.append("📄 **相关文档**:\n");
        context.append("- 项目架构文档: 描述了系统的整体设计和模块划分\n");
        context.append("- 编码规范: 定义了代码风格和最佳实践\n");
        context.append("- API设计指南: 说明了接口设计原则\n\n");
        
        // 模拟相关代码
        context.append("💻 **相关代码文件**:\n");
        context.append("- 核心服务类: 包含主要业务逻辑实现\n");
        context.append("- 配置文件: 系统配置和依赖管理\n");
        context.append("- 测试用例: 相关功能的单元测试\n\n");
        
        context.append("⚠️ **注意**: 当前为模拟模式，请启用Memory服务获取真实上下文\n");
        context.append("🏷️ **仓库**: ").append(repositoryId).append("\n");
        context.append("=== 上下文信息结束 ===\n\n");
        
        return context.toString();
    }
}