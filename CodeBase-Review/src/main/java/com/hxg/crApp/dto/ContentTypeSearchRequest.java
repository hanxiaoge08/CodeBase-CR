package com.hxg.crApp.dto;

/**
 * 内容类型搜索请求DTO
 * 
 * @author AI Assistant
 */
public class ContentTypeSearchRequest {
    
    private String repositoryId;
    private String query;
    private String contentType;
    private int limit = 10;
    
    public ContentTypeSearchRequest() {}
    
    public ContentTypeSearchRequest(String repositoryId, String query, String contentType, int limit) {
        this.repositoryId = repositoryId;
        this.query = query;
        this.contentType = contentType;
        this.limit = limit;
    }
    
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