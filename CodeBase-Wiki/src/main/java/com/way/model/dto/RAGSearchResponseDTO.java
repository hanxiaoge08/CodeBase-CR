package com.way.model.dto;

import lombok.Data;

import java.util.List;

/**
 * RAG搜索响应DTO
 * @author way
 */
@Data
public class RAGSearchResponseDTO {
    private String query;                     // 用户查询
    private List<SearchResultDTO> searchResults; // 检索结果
    private String aiResponse;                // AI回答
    private Integer resultCount;              // 结果数量
    private Boolean hasContext;               // 是否有上下文
}
