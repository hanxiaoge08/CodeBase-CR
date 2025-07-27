package com.hxg.memory.controller;

import com.hxg.memory.service.CodeReviewMemoryService;
import com.hxg.memory.service.DocumentMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 搜索API控制器
 * 提供多种搜索方式的接口
 * 
 * @author AI Assistant
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    @Autowired
    private CodeReviewMemoryService codeReviewMemoryService;
    
    @Autowired
    private DocumentMemoryService documentMemoryService;
    
    /**
     * 代码评审上下文搜索
     */
    @PostMapping("/code-review")
    public ResponseEntity<String> searchCodeReviewContext(@RequestBody CodeReviewSearchRequest request) {
        
        try {
            logger.info("代码评审上下文搜索: repositoryId={}, prTitle={}", 
                request.getRepositoryId(), request.getPrTitle());
            
            String result = codeReviewMemoryService.searchContextForCodeReview(
                request.getRepositoryId(),
                request.getDiffContent(),
                request.getPrTitle(),
                request.getPrDescription(),
                request.getChangedFiles()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("代码评审上下文搜索失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().body("搜索失败: " + e.getMessage());
        }
    }
    
    /**
     * 按内容类型搜索
     */
    @PostMapping("/by-content-type")
    public ResponseEntity<List<CodeReviewMemoryService.MemorySearchResult>> searchByContentType(
            @RequestBody ContentTypeSearchRequest request) {
        
        try {
            logger.info("按内容类型搜索: repositoryId={}, contentType={}, query={}", 
                request.getRepositoryId(), request.getContentType(), request.getQuery());
            
            List<CodeReviewMemoryService.MemorySearchResult> results = 
                codeReviewMemoryService.searchByContentType(
                    request.getRepositoryId(),
                    request.getQuery(),
                    request.getContentType(),
                    request.getLimit()
                );
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("按内容类型搜索失败: repositoryId={}, contentType={}", 
                request.getRepositoryId(), request.getContentType(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 搜索相关文档
     */
    @PostMapping("/documents")
    public ResponseEntity<List<CodeReviewMemoryService.MemorySearchResult>> searchDocuments(
            @RequestBody DocumentSearchRequest request) {
        
        try {
            logger.info("搜索相关文档: repositoryId={}, query={}", 
                request.getRepositoryId(), request.getQuery());
            
            List<CodeReviewMemoryService.MemorySearchResult> results = 
                codeReviewMemoryService.searchRelatedDocuments(
                    request.getRepositoryId(),
                    request.getQuery(),
                    request.getLimit()
                );
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("搜索文档失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 搜索相关代码文件
     */
    @PostMapping("/code-files")
    public ResponseEntity<List<CodeReviewMemoryService.MemorySearchResult>> searchCodeFiles(
            @RequestBody CodeFileSearchRequest request) {
        
        try {
            logger.info("搜索相关代码文件: repositoryId={}, query={}", 
                request.getRepositoryId(), request.getQuery());
            
            List<CodeReviewMemoryService.MemorySearchResult> results = 
                codeReviewMemoryService.searchRelatedCodeFiles(
                    request.getRepositoryId(),
                    request.getQuery(),
                    request.getLimit()
                );
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("搜索代码文件失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量添加文档到记忆
     */
    @PostMapping("/index/documents")
    public ResponseEntity<String> indexDocuments(@RequestBody BatchIndexRequest request) {
        
        try {
            logger.info("批量索引文档: repositoryId={}, documentCount={}", 
                request.getRepositoryId(), request.getDocuments().size());
            
            CompletableFuture<Void> future = documentMemoryService.batchAddDocumentMemoriesAsync(
                request.getRepositoryId(),
                request.getDocuments()
            );
            
            // 异步处理，立即返回
            return ResponseEntity.accepted().body("文档索引任务已启动");
            
        } catch (Exception e) {
            logger.error("批量索引文档失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().body("索引失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加单个文档到记忆
     */
    @PostMapping("/index/document")
    public ResponseEntity<String> indexDocument(@RequestBody SingleIndexRequest request) {
        
        try {
            logger.info("索引单个文档: repositoryId={}, documentName={}", 
                request.getRepositoryId(), request.getDocumentName());
            
            CompletableFuture<Void> future = documentMemoryService.addDocumentMemoryAsync(
                request.getRepositoryId(),
                request.getDocumentName(),
                request.getDocumentContent(),
                request.getDocumentUrl(),
                request.getMetadata()
            );
            
            // 异步处理，立即返回
            return ResponseEntity.accepted().body("文档索引任务已启动");
            
        } catch (Exception e) {
            logger.error("索引文档失败: repositoryId={}, documentName={}", 
                request.getRepositoryId(), request.getDocumentName(), e);
            return ResponseEntity.internalServerError().body("索引失败: " + e.getMessage());
        }
    }
    
    /**
     * 代码评审搜索请求
     */
    public static class CodeReviewSearchRequest {
        private String repositoryId;
        private String diffContent;
        private String prTitle;
        private String prDescription;
        private List<String> changedFiles;
        
        // Getters and Setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public String getDiffContent() { return diffContent; }
        public void setDiffContent(String diffContent) { this.diffContent = diffContent; }
        
        public String getPrTitle() { return prTitle; }
        public void setPrTitle(String prTitle) { this.prTitle = prTitle; }
        
        public String getPrDescription() { return prDescription; }
        public void setPrDescription(String prDescription) { this.prDescription = prDescription; }
        
        public List<String> getChangedFiles() { return changedFiles; }
        public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }
    }
    
    /**
     * 内容类型搜索请求
     */
    public static class ContentTypeSearchRequest {
        private String repositoryId;
        private String query;
        private String contentType;
        private int limit = 5;
        
        // Getters and Setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
    
    /**
     * 文档搜索请求
     */
    public static class DocumentSearchRequest {
        private String repositoryId;
        private String query;
        private int limit = 5;
        
        // Getters and Setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
    
    /**
     * 代码文件搜索请求
     */
    public static class CodeFileSearchRequest {
        private String repositoryId;
        private String query;
        private int limit = 3;
        
        // Getters and Setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
    
    /**
     * 批量索引请求
     */
    public static class BatchIndexRequest {
        private String repositoryId;
        private List<DocumentMemoryService.DocumentInfo> documents;
        
        // Getters and Setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public List<DocumentMemoryService.DocumentInfo> getDocuments() { return documents; }
        public void setDocuments(List<DocumentMemoryService.DocumentInfo> documents) { this.documents = documents; }
    }
    
    /**
     * 单个索引请求
     */
    public static class SingleIndexRequest {
        private String repositoryId;
        private String documentName;
        private String documentContent;
        private String documentUrl;
        private Map<String, Object> metadata;
        
        // Getters and Setters
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public String getDocumentName() { return documentName; }
        public void setDocumentName(String documentName) { this.documentName = documentName; }
        
        public String getDocumentContent() { return documentContent; }
        public void setDocumentContent(String documentContent) { this.documentContent = documentContent; }
        
        public String getDocumentUrl() { return documentUrl; }
        public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
} 