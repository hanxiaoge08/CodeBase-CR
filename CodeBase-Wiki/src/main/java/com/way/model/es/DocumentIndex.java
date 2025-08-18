package com.way.model.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 文档索引模型
 * 对应第二个ES索引结构
 * @author way
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentIndex {
    
    @JsonProperty("repoId")
    private String repoId;
    
    @JsonProperty("taskId")
    private String taskId;
    
    @JsonProperty("catalogueId")
    private String catalogueId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("sha256")
    private String sha256;
    
    @JsonProperty("mtime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime mtime;
    
    @JsonProperty("status")
    private Integer status;
    
    @JsonProperty("vector")
    private float[] vector;

    // 构造函数
    public DocumentIndex() {}

    public DocumentIndex(String repoId, String taskId, String catalogueId, String name) {
        this.repoId = repoId;
        this.taskId = taskId;
        this.catalogueId = catalogueId;
        this.name = name;
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

    public String getCatalogueId() {
        return catalogueId;
    }

    public void setCatalogueId(String catalogueId) {
        this.catalogueId = catalogueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

    @Override
    public String toString() {
        return "DocumentIndex{" +
                "repoId='" + repoId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", catalogueId='" + catalogueId + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
