package com.way.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 代码评审上下文搜索请求DTO
 * 
 * @author way
 */
@Setter
@Getter
public class CodeReviewContextRequest {

    // Getters and Setters
    @JsonProperty("repositoryId")
    private String repositoryId;
    
    @JsonProperty("taskId")
    private String taskId;
    
    @JsonProperty("diffContent")
    private String diffContent;
    
    @JsonProperty("prTitle")
    private String prTitle;
    
    @JsonProperty("prDescription")
    private String prDescription;
    
    @JsonProperty("changedFiles")
    private List<String> changedFiles;
    
    @JsonProperty("maxResults")
    private Integer maxResults = 10;
    
    public CodeReviewContextRequest() {}
    
    public CodeReviewContextRequest(String repositoryId, String taskId, String diffContent, 
                                   String prTitle, String prDescription, List<String> changedFiles) {
        this.repositoryId = repositoryId;
        this.taskId = taskId;
        this.diffContent = diffContent;
        this.prTitle = prTitle;
        this.prDescription = prDescription;
        this.changedFiles = changedFiles;
    }

    @Override
    public String toString() {
        return "CodeReviewContextRequest{" +
                "repositoryId='" + repositoryId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", prTitle='" + prTitle + '\'' +
                ", changedFiles=" + changedFiles +
                ", maxResults=" + maxResults +
                '}';
    }
}