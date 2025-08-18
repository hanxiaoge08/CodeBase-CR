package com.way.model.dto;

/**
 * 代码文件记忆请求DTO
 * 
 * @author AI Assistant
 */
public class CodeFileRequest {
    
    private String repositoryId;
    private String fileName;
    private String filePath;
    private String fileContent;
    private String fileType;
    
    public CodeFileRequest() {}
    
    public CodeFileRequest(String repositoryId, String fileName, String filePath, 
                          String fileContent, String fileType) {
        this.repositoryId = repositoryId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileContent = fileContent;
        this.fileType = fileType;
    }
    
    // Getters and Setters
    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileContent() { return fileContent; }
    public void setFileContent(String fileContent) { this.fileContent = fileContent; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}