package com.way.crApp.dto.review;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 审查结果 DTO
 * 
 * 包含一个 List<ReviewCommentDTO>，用于 BeanOutputParser 解析LLM的结构化输出
 */
public record ReviewResultDTO(
        @JsonProperty("comments")
        List<ReviewCommentDTO> comments,
        
        @JsonProperty("summary")
        String summary,
        
        @JsonProperty("overall_rating")
        String overallRating
) {
    
    /**
     * 整体评级枚举
     */
    public enum OverallRating {
        EXCELLENT("excellent"),
        GOOD("good"),
        NEEDS_IMPROVEMENT("needs_improvement"),
        POOR("poor");
        
        private final String value;
        
        OverallRating(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * 构建器模式创建ReviewResultDTO
     */
    public static class Builder {
        private List<ReviewCommentDTO> comments;
        private String summary;
        private String overallRating = OverallRating.GOOD.getValue();
        
        public Builder comments(List<ReviewCommentDTO> comments) {
            this.comments = comments;
            return this;
        }
        
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder overallRating(String overallRating) {
            this.overallRating = overallRating;
            return this;
        }
        
        public Builder overallRating(OverallRating overallRating) {
            this.overallRating = overallRating.getValue();
            return this;
        }
        
        public ReviewResultDTO build() {
            return new ReviewResultDTO(comments, summary, overallRating);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
} 