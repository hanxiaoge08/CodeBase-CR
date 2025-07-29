package com.hxg.model.dto;

import java.util.List;

/**
 * 批量文档请求DTO
 * 
 * @author AI Assistant
 */
public class BatchDocumentRequest {
    
    private String repositoryId;
    private List<DocumentInfo> documents;
    
    public BatchDocumentRequest() {}
    
    public BatchDocumentRequest(String repositoryId, List<DocumentInfo> documents) {
        this.repositoryId = repositoryId;
        this.documents = documents;
    }
    
    // Getters and Setters
    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
    
    public List<DocumentInfo> getDocuments() { return documents; }
    public void setDocuments(List<DocumentInfo> documents) { this.documents = documents; }
    
    /**
     * 文档信息类
     */
    public static class DocumentInfo {
        private String name;
        private String content;
        private String url;
        private java.util.Map<String, Object> metadata;
        
        public DocumentInfo() {}
        
        public DocumentInfo(String name, String content, String url) {
            this.name = name;
            this.content = content;
            this.url = url;
        }
        
        public DocumentInfo(String name, String content, String url, java.util.Map<String, Object> metadata) {
            this.name = name;
            this.content = content;
            this.url = url;
            this.metadata = metadata;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public java.util.Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
    }
}