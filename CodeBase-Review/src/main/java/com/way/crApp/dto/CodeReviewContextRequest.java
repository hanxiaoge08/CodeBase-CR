package com.way.crApp.dto;

import java.util.List;

/**
 * 代码评审上下文搜索请求DTO
 * 
 * @author AI Assistant
 */
public class CodeReviewContextRequest {
    
    private String repositoryId;
    private String taskId;
    private String diffContent;
    private String prTitle;
    private String prDescription;
    private List<String> changedFiles;
    
    public CodeReviewContextRequest() {}
    
    public CodeReviewContextRequest(String repositoryId, String diffContent, String prTitle, 
                                   String prDescription, List<String> changedFiles) {
        this.repositoryId = repositoryId;
        this.diffContent = diffContent;
        this.prTitle = prTitle;
        this.prDescription = prDescription;
        this.changedFiles = changedFiles;
    }
    
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}