package com.way.model.dto;

import lombok.Data;

/**
 * 工具调用响应DTO
 * @author way
 */
@Data
public class ToolCallResponseDTO {
    private String query;              // 用户查询
    private String withoutToolsResult; // 不使用工具的结果
    private String withToolsResult;    // 使用工具的结果
}
