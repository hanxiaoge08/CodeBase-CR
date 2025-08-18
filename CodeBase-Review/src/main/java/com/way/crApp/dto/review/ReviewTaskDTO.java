package com.way.crApp.dto.review;

/**
 * 审查任务 DTO
 * 
 * 核心任务对象，包含 repoFullName, prNumber, diffUrl 等信息，在系统内部流转
 */
public record ReviewTaskDTO(
        String repositoryFullName,
        Integer prNumber,
        String diffUrl,
        String prTitle,
        String prAuthor,
        String headSha,
        String baseSha,
        String cloneUrl
) {
    
    /**
     * 构建器模式创建ReviewTaskDTO
     */
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
            return new ReviewTaskDTO(
                repositoryFullName, 
                prNumber, 
                diffUrl, 
                prTitle, 
                prAuthor, 
                headSha, 
                baseSha, 
                cloneUrl
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
} 