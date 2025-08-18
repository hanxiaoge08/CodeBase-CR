package com.way.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 纯搜索响应DTO
 * @author way
 */
@Data
public class SearchOnlyResponseDTO {
    private String query;                     // 用户查询
    private List<SearchResultDTO> searchResults; // 检索结果
    private Integer resultCount;              // 结果数量
}
