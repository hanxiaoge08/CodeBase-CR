package com.hxg.model.context;

import com.hxg.model.entity.Task;
import com.hxg.model.param.CreateTaskParams;
import lombok.Data;

/**
 * @author hxg
 * @description: 执行上下文
 * @date 2025/7/20 23:22
 */
@Data
public class ExecutionContext {
    private String taskId;

    private Task task;

    private CreateTaskParams createParams;

    private String localPath;
}
