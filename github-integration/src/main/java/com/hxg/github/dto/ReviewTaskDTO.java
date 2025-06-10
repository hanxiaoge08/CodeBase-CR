package com.hxg.github.dto;

/**
 * 代码审查任务 DTO
 * 
 * 用于封装代码审查任务的所有必要信息
 */
public class ReviewTaskDTO {
    private String repositoryFullName;
    private Integer prNumber;
    private String diffUrl;
    private String prTitle;
    private String prAuthor;
    private String headSha;
    private String baseSha;
    private String cloneUrl;

    // 私有构造器，只能通过Builder创建
    private ReviewTaskDTO(Builder builder) {
        this.repositoryFullName = builder.repositoryFullName;
        this.prNumber = builder.prNumber;
        this.diffUrl = builder.diffUrl;
        this.prTitle = builder.prTitle;
        this.prAuthor = builder.prAuthor;
        this.headSha = builder.headSha;
        this.baseSha = builder.baseSha;
        this.cloneUrl = builder.cloneUrl;
    }

    // Getters
    public String repositoryFullName() { return repositoryFullName; }
    public Integer prNumber() { return prNumber; }
    public String diffUrl() { return diffUrl; }
    public String prTitle() { return prTitle; }
    public String prAuthor() { return prAuthor; }
    public String headSha() { return headSha; }
    public String baseSha() { return baseSha; }
    public String cloneUrl() { return cloneUrl; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String repositoryFullName;
        private Integer prNumber;
        private String diffUrl;
        private String prTitle;
        private String prAuthor;
        private String headSha;
        private String baseSha;
        private String cloneUrl;

        public Builder repositoryFullName(String repositoryFullName) {
            this.repositoryFullName = repositoryFullName;
            return this;
        }

        public Builder prNumber(Integer prNumber) {
            this.prNumber = prNumber;
            return this;
        }

        public Builder diffUrl(String diffUrl) {
            this.diffUrl = diffUrl;
            return this;
        }

        public Builder prTitle(String prTitle) {
            this.prTitle = prTitle;
            return this;
        }

        public Builder prAuthor(String prAuthor) {
            this.prAuthor = prAuthor;
            return this;
        }

        public Builder headSha(String headSha) {
            this.headSha = headSha;
            return this;
        }

        public Builder baseSha(String baseSha) {
            this.baseSha = baseSha;
            return this;
        }

        public Builder cloneUrl(String cloneUrl) {
            this.cloneUrl = cloneUrl;
            return this;
        }

        public ReviewTaskDTO build() {
            return new ReviewTaskDTO(this);
        }
    }
} 