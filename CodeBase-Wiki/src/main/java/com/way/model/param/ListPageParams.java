package com.way.model.param;

import lombok.Data;

/**
 * @author way
 * @description: 分页查询参数
 * @date 2025/7/23 16:11
 */
@Data
public class ListPageParams {

    private Integer pageIndex = 1;

    private Integer pageSize = 10;

    private String projectName;

    private String taskId;

    private String userName;

}