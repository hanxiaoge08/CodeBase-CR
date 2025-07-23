package com.hxg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hxg.model.entity.Catalogue;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author hxg
 * @description: 目录Mapper
 * @date 2025/7/23 00:30
 */
@Mapper
public interface CatalogueMapper extends BaseMapper<Catalogue> {
}