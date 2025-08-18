package com.way.crApp.service;

import com.way.crApp.client.ESWikiServiceClient;
import com.way.crApp.dto.CodeReviewContextRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Review模块的记忆服务
 * 专门处理PR代码评审的上下文检索
 * 通过Feign客户端调用ES模块的REST API
 * 
 * @author AI Assistant
 */
@Service
public class ReviewESService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewESService.class);

    @Autowired
    private ESWikiServiceClient esWikiServiceClient;
    
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

            //改为调用wiki服务的
            String context = esWikiServiceClient.searchContextForCodeReview(request);
            
            logger.info("代码评审上下文搜索完成: repositoryId={}", repositoryId);
            return context;
            
        } catch (Exception e) {
            logger.error("搜索代码评审上下文失败: repositoryId={}", repositoryId, e);
            return "无法获取相关上下文信息，将基于diff内容进行评审。";
        }
    }

    /**
     * 检查记忆服务是否可用
     */
    public boolean isWikiServiceAvailable() {
        try {
            Map<String, Object> result = esWikiServiceClient.healthCheck();
            boolean available = result != null && "UP".equals(result.get("status"));
            logger.debug("记忆服务可用性检查: {}", available);
            return available;
        } catch (Exception e) {
            logger.debug("记忆服务健康检查失败: {}", e.getMessage());
            return false;
        }
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