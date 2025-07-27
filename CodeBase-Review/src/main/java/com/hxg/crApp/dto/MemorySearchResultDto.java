package com.hxg.crApp.dto;

import java.util.List;
import java.util.Map;

/**
 * 记忆搜索结果DTO
 * 
 * @author AI Assistant
 */
public class MemorySearchResultDto {
    
    private String id;
    private String content;
    private Double score;
    private String type;
    private String name;
    private Map<String, Object> metadata;
    
    public MemorySearchResultDto() {}
    
    public MemorySearchResultDto(String id, String content, Double score, String type, 
                                String name, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.score = score;
        this.type = type;
        this.name = name;
        this.metadata = metadata;
    }
    
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
    
    /**
     * 搜索结果响应包装类
     */
    public static class SearchResponse {
        private List<MemorySearchResultDto> results;
        private int totalCount;
        
        public SearchResponse() {}
        
        public SearchResponse(List<MemorySearchResultDto> results, int totalCount) {
            this.results = results;
            this.totalCount = totalCount;
        }
        
        public List<MemorySearchResultDto> getResults() { return results; }
        public void setResults(List<MemorySearchResultDto> results) { this.results = results; }
        
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }
}