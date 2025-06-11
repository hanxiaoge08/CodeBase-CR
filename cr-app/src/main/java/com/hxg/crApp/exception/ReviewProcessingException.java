package com.hxg.crApp.exception;

/**
 * 审查处理异常类
 * 
 * 用于处理代码审查过程中的异常
 */
public class ReviewProcessingException extends RuntimeException {

    private final String repositoryName;
    private final Integer prNumber;

    public ReviewProcessingException(String message) {
        super(message);
        this.repositoryName = null;
        this.prNumber = null;
    }

    public ReviewProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.repositoryName = null;
        this.prNumber = null;
    }

    public ReviewProcessingException(String repositoryName, Integer prNumber, String message) {
        super(message);
        this.repositoryName = repositoryName;
        this.prNumber = prNumber;
    }

    public ReviewProcessingException(String repositoryName, Integer prNumber, String message, Throwable cause) {
        super(message, cause);
        this.repositoryName = repositoryName;
        this.prNumber = prNumber;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public Integer getPrNumber() {
        return prNumber;
    }

    @Override
    public String getMessage() {
        if (repositoryName != null && prNumber != null) {
            return String.format("[%s#%d] %s", repositoryName, prNumber, super.getMessage());
        }
        return super.getMessage();
    }
} 