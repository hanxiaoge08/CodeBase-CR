package com.hxg.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.hxg.model.enums.TaskStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author hxg
 * @description: 任务表
 * @date 2025/7/20 23:22
 */
@Data
@Builder
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String projectName;

    private String projectUrl;

    private String userName;

    private TaskStatusEnum status;

    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
