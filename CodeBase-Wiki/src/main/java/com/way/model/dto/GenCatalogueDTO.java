package com.way.model.dto;

import com.way.model.entity.Catalogue;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author way
 * @description: 目录生成结果
 * @date 2025/7/20 23:21
 */
@Data
@AllArgsConstructor
public class GenCatalogueDTO {
    private CatalogueStruct catalogueStruct;

    private List<Catalogue> catalogueList;
}
