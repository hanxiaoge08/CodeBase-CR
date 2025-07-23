package com.hxg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hxg.model.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author hxg
 * @description: 任务映射类
 * @date 2025/7/23 16:05
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}