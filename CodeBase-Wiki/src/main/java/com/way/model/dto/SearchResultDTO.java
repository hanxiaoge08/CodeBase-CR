package com.way.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 检索结果DTO
 * @author way
 */
public class SearchResultDTO {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("score")
    private Double score;
    
    @JsonProperty("type")
    private String type; // "code" 或 "document"
    
    @JsonProperty("repoId")
    private String repoId;
    
    @JsonProperty("taskId")
    private String taskId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("summary")
    private String summary;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("className")
    private String className;
    
    @JsonProperty("methodName")
    private String methodName;
    
    @JsonProperty("apiName")
    private String apiName;

    public SearchResultDTO() {}

    public SearchResultDTO(String id, Double score, String type, String content) {
        this.id = id;
        this.score = score;
        this.type = type;
        this.content = content;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    @Override
    public String toString() {
        return "SearchResultDTO{" +
                "id='" + id + '\'' +
                ", score=" + score +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", apiName='" + apiName + '\'' +
                '}';
    }
}
