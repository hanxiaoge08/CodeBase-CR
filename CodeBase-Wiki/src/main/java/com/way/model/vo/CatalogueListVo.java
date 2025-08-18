package com.way.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author way
 * @description: 目录列表
 * @date 2025/7/22 22:46
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CatalogueListVo {
    private String catalogueId;
    private String parentCatalogueId;
    private String name;
    private String title;
    private String prompt;
    private String dependentFile;
    private List<CatalogueListVo> children;
    private String content;
    private Integer status;
}
