package com.way.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 代码解析响应DTO
 * @author way
 */
public class CodeParseResponse {
    
    @JsonProperty("chunks")
    private List<CodeChunkDto> chunks;

    public CodeParseResponse() {}

    public List<CodeChunkDto> getChunks() {
        return chunks;
    }

    public void setChunks(List<CodeChunkDto> chunks) {
        this.chunks = chunks;
    }

    public static class CodeChunkDto {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("language")
        private String language;
        
        @JsonProperty("subType")
        private String subType;
        
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

        public CodeChunkDto() {}

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getSubType() {
            return subType;
        }

        public void setSubType(String subType) {
            this.subType = subType;
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
        }

        @Override
        public String toString() {
            return "CodeChunkDto{" +
                    "id='" + id + '\'' +
                    ", language='" + language + '\'' +
                    ", subType='" + subType + '\'' +
                    ", className='" + className + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", apiName='" + apiName + '\'' +
                    '}';
        }
    }
}
