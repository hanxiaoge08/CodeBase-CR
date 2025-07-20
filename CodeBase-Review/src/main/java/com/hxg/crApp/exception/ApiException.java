package com.hxg.crApp.exception;

/**
 * API异常类
 * 
 * 用于处理API调用相关的异常
 */
public class ApiException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public ApiException(String message) {
        super(message);
        this.errorCode = "API_ERROR";
        this.httpStatus = 500;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "API_ERROR";
        this.httpStatus = 500;
    }

    public ApiException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ApiException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
} 