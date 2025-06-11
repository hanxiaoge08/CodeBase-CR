package com.hxg.crApp.dto.review;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 审查评论 DTO
 * 
 * AI生成的单条评论，包含 filePath, lineNumber, comment
 */
public record ReviewCommentDTO(
        @JsonProperty("file_path")
        String filePath,
        
        @JsonProperty("line_number")
        Integer lineNumber,
        
        @JsonProperty("comment")
        String comment,
        
        @JsonProperty("severity")
        String severity
) {
    
    /**
     * 评论严重程度枚举
     */
    public enum Severity {
        INFO("info"),
        WARNING("warning"),
        ERROR("error");
        
        private final String value;
        
        Severity(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * 构建器模式创建ReviewCommentDTO
     */
    public static class Builder {
        private String filePath;
        private Integer lineNumber;
        private String comment;
        private String severity = Severity.INFO.getValue();
        
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
        
        public Builder lineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }
        
        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity.getValue();
            return this;
        }
        
        public ReviewCommentDTO build() {
            return new ReviewCommentDTO(filePath, lineNumber, comment, severity);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
} 