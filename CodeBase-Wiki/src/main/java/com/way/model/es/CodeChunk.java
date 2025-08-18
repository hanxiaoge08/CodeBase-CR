package com.way.model.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 代码块索引模型
 * 对应第一个ES索引结构
 * @author way
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeChunk {
    
    @JsonProperty("repoId")
    private String repoId;
    
    @JsonProperty("taskId")
    private String taskId;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("className")
    private String className;
    
    @JsonProperty("methodName")
    private String methodName;
    
    @JsonProperty("apiName")
    private String apiName;
    
    @JsonProperty("docSummary")
    private String docSummary;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("sha256")
    private String sha256;
    
    @JsonProperty("mtime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime mtime;
    
    @JsonProperty("chunk_size")
    private Integer chunkSize;
    
    @JsonProperty("vector")
    private float[] vector;

    // 构造函数
    public CodeChunk() {}

    public CodeChunk(String repoId, String taskId, String language) {
        this.repoId = repoId;
        this.taskId = taskId;
        this.language = language;
        this.mtime = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getDocSummary() {
        return docSummary;
    }

    public void setDocSummary(String docSummary) {
        this.docSummary = docSummary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.chunkSize = content != null ? content.length() : 0;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public LocalDateTime getMtime() {
        return mtime;
    }

    public void setMtime(LocalDateTime mtime) {
        this.mtime = mtime;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

    @Override
    public String toString() {
        return "CodeChunk{" +
                "repoId='" + repoId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", language='" + language + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", apiName='" + apiName + '\'' +
                ", chunkSize=" + chunkSize +
                '}';
    }
}
