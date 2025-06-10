package com.hxg.github.dto;

/**
 * 代码审查评论 DTO
 * 
 * 用于封装行级评论信息
 */
public record ReviewCommentDTO(
        String comment,
        String filePath,
        Integer lineNumber
) {} 