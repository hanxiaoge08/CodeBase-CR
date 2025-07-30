package com.hxg.crApp.service;

import com.hxg.crApp.client.MemoryServiceClient;
import com.hxg.crApp.dto.CodeReviewContextRequest;
import com.hxg.crApp.dto.ContentTypeSearchRequest;
import com.hxg.crApp.dto.MemorySearchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Review模块的记忆服务
 * 专门处理PR代码评审的上下文检索
 * 通过Feign客户端调用Memory模块的REST API
 * 
 * @author AI Assistant
 */
@Service
public class ReviewMemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewMemoryService.class);
    
    @Autowired
    private MemoryServiceClient memoryServiceClient;
    
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
            CodeReviewContextRequest request = new CodeReviewContextRequest(
                repositoryId, diffContent, prTitle, prDescription, changedFiles);
            
            String context = memoryServiceClient.searchContextForCodeReview(request);
            
            logger.info("代码评审上下文搜索完成: repositoryId={}", repositoryId);
            return context;
            
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
            ContentTypeSearchRequest request = new ContentTypeSearchRequest(repositoryId, query, contentType, limit);
            MemorySearchResultDto.SearchResponse response = memoryServiceClient.searchByContentType(request);
            
            return response.getResults().stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
                
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
            ContentTypeSearchRequest request = new ContentTypeSearchRequest(repositoryId, query, "document", limit);
            MemorySearchResultDto.SearchResponse response = memoryServiceClient.searchRelatedDocuments(request);
            
            return response.getResults().stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
                
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
            ContentTypeSearchRequest request = new ContentTypeSearchRequest(repositoryId, query, "code_file", limit);
            MemorySearchResultDto.SearchResponse response = memoryServiceClient.searchRelatedCodeFiles(request);
            
            return response.getResults().stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("搜索相关代码文件失败: repositoryId={}", repositoryId, e);
            return List.of();
        }
    }
    
    /**
     * 检查记忆服务是否可用
     */
    public boolean isMemoryServiceAvailable() {
        try {
            Map<String, Object> result = memoryServiceClient.healthCheck();
            boolean available = result != null && "UP".equals(result.get("status"));
            logger.debug("记忆服务可用性检查: {}", available);
            return available;
        } catch (Exception e) {
            logger.debug("记忆服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 转换搜索结果格式
     */
    private SearchResult convertToSearchResult(MemorySearchResultDto memoryResult) {
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