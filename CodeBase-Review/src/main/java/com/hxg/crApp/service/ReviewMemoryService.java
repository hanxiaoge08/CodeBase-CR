package com.hxg.crApp.service;

import com.alibaba.example.chatmemory.service.CodeReviewMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Review模块的记忆服务
 * 专门处理PR代码评审的上下文检索
 * 
 * @author AI Assistant
 */
@Service
@ConditionalOnClass(CodeReviewMemoryService.class)
public class ReviewMemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewMemoryService.class);
    
    @Autowired(required = false)
    private CodeReviewMemoryService codeReviewMemoryService;
    
    /**
     * 为PR代码评审搜索相关上下文
     * 
     * @param repositoryId 仓库ID
     * @param diffContent PR的diff内容
     * @param prTitle PR标题
     * @param prDescription PR描述
     * @param changedFiles 变更的文件列表
     * @return 格式化的上下文信息
     */
    public String searchContextForCodeReview(String repositoryId,
                                           String diffContent,
                                           String prTitle,
                                           String prDescription,
                                           List<String> changedFiles) {
        
        logger.info("开始为代码评审搜索上下文: repositoryId={}", repositoryId);
        
        try {
            if (codeReviewMemoryService != null) {
                // 调用实际的CodeReviewMemoryService
                return codeReviewMemoryService.searchContextForCodeReview(
                    repositoryId, diffContent, prTitle, prDescription, changedFiles);
            } else {
                logger.warn("CodeReviewMemoryService不可用，使用模拟上下文");
                return buildMockContextResponse(repositoryId, prTitle, diffContent);
            }
            
        } catch (Exception e) {
            logger.error("搜索代码评审上下文失败: repositoryId={}", repositoryId, e);
            return "无法获取相关上下文信息，将基于diff内容进行评审。";
        }
    }
    
    /**
     * 搜索特定类型的内容
     * 
     * @param repositoryId 仓库ID
     * @param query 搜索查询
     * @param contentType 内容类型 (document, code_file)
     * @param limit 返回结果数量限制
     * @return 搜索结果
     */
    public List<SearchResult> searchByContentType(String repositoryId,
                                                 String query,
                                                 String contentType,
                                                 int limit) {
        
        logger.debug("按内容类型搜索: repositoryId={}, contentType={}, query={}", 
            repositoryId, contentType, query);
        
        try {
            if (codeReviewMemoryService != null) {
                // 调用实际的CodeReviewMemoryService并转换结果
                List<CodeReviewMemoryService.MemorySearchResult> results = 
                    codeReviewMemoryService.searchByContentType(repositoryId, query, contentType, limit);
                
                return results.stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());
            } else {
                logger.debug("CodeReviewMemoryService不可用，返回空结果");
                return List.of();
            }
            
        } catch (Exception e) {
            logger.error("按内容类型搜索失败: repositoryId={}, contentType={}", repositoryId, contentType, e);
            return List.of();
        }
    }
    
    /**
     * 搜索相关文档
     */
    public List<SearchResult> searchRelatedDocuments(String repositoryId, String query, int limit) {
        logger.debug("搜索相关文档: repositoryId={}, query={}", repositoryId, query);
        
        try {
            if (codeReviewMemoryService != null) {
                List<CodeReviewMemoryService.MemorySearchResult> results = 
                    codeReviewMemoryService.searchRelatedDocuments(repositoryId, query, limit);
                
                return results.stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());
            } else {
                return List.of();
            }
        } catch (Exception e) {
            logger.error("搜索相关文档失败: repositoryId={}", repositoryId, e);
            return List.of();
        }
    }
    
    /**
     * 搜索相关代码文件
     */
    public List<SearchResult> searchRelatedCodeFiles(String repositoryId, String query, int limit) {
        logger.debug("搜索相关代码文件: repositoryId={}, query={}", repositoryId, query);
        
        try {
            if (codeReviewMemoryService != null) {
                List<CodeReviewMemoryService.MemorySearchResult> results = 
                    codeReviewMemoryService.searchRelatedCodeFiles(repositoryId, query, limit);
                
                return results.stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());
            } else {
                return List.of();
            }
        } catch (Exception e) {
            logger.error("搜索相关代码文件失败: repositoryId={}", repositoryId, e);
            return List.of();
        }
    }
    
    /**
     * 检查记忆服务是否可用
     */
    public boolean isMemoryServiceAvailable() {
        boolean available = codeReviewMemoryService != null;
        logger.debug("记忆服务可用性检查: {}", available);
        return available;
    }
    
    /**
     * 转换搜索结果格式
     */
    private SearchResult convertToSearchResult(CodeReviewMemoryService.MemorySearchResult memoryResult) {
        SearchResult result = new SearchResult();
        result.setId(memoryResult.getId());
        result.setContent(memoryResult.getContent());
        result.setScore(memoryResult.getScore());
        result.setType(memoryResult.getType());
        result.setName(memoryResult.getName());
        result.setMetadata(memoryResult.getMetadata());
        return result;
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
        
        context.append("⚠️ **注意**: 当前为模拟模式，请启用Mem0服务获取真实上下文\n");
        context.append("🏷️ **仓库**: ").append(repositoryId).append("\n");
        context.append("=== 上下文信息结束 ===\n\n");
        
        return context.toString();
    }
    
    /**
     * 搜索结果内部类
     */
    public static class SearchResult {
        private String id;
        private String content;
        private Double score;
        private String type;
        private String name;
        private Map<String, Object> metadata;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
} 