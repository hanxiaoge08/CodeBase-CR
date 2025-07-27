package com.hxg.memory.dto;

import java.util.Map;

/**
 * 文档记忆请求DTO
 * 
 * @author AI Assistant
 */
public class DocumentRequest {
    
    private String repositoryId;
    private String documentName;
    private String documentContent;
    private String documentUrl;
    private Map<String, Object> metadata;
    
    public DocumentRequest() {}
    
    public DocumentRequest(String repositoryId, String documentName, String documentContent, 
                          String documentUrl, Map<String, Object> metadata) {
        this.repositoryId = repositoryId;
        this.documentName = documentName;
        this.documentContent = documentContent;
        this.documentUrl = documentUrl;
        this.metadata = metadata;
    }
    
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