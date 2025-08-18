package com.way.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.way.model.entity.Catalogue;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author way
 * @description: 目录Mapper
 * @date 2025/7/23 00:30
 */
@Mapper
public interface CatalogueMapper extends BaseMapper<Catalogue> {
}