package com.way.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 代码解析请求DTO
 * @author way
 */
public class CodeParseRequest {
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("max_chars")
    private Integer maxChars = 1000;

    public CodeParseRequest() {}

    public CodeParseRequest(String language, String code) {
        this.language = language;
        this.code = code;
    }

    public CodeParseRequest(String language, String code, Integer maxChars) {
        this.language = language;
        this.code = code;
        this.maxChars = maxChars;
    }

    // Getters and Setters
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getMaxChars() {
        return maxChars;
    }

    public void setMaxChars(Integer maxChars) {
        this.maxChars = maxChars;
    }
}
