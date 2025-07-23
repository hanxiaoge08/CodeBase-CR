package com.hxg.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author hxg
 * @description: 目录实体类
 * @date 2025/7/22 22:36
 */
@Data
@TableName("Catalogue")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Catalogue {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private String catalogueId;

    private String parentCatalogueId;

    private String name;

    private String title;

    private String prompt;

    private String dependentFile;

    private String children;

    private String content;

    private Integer status;

    private String failReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}