package com.way.model.dto;

import lombok.Data;

/**
 * 聊天响应DTO
 * @author way
 */
@Data
public class ChatResponseDTO {
    private String query;           // 用户查询
    private String response;        // AI回答
    private Boolean useRAG;         // 是否使用RAG
    private Boolean hasContext;     // 是否有上下文
    private Integer contextCount;   // 上下文数量
}
