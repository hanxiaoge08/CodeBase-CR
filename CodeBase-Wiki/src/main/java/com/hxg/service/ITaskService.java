package com.hxg.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hxg.model.entity.Task;
import com.hxg.model.param.CreateTaskParams;
import com.hxg.model.param.ListPageParams;
import com.hxg.model.vo.TaskVo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author hxg
 * @description: TODO
 * @date 2025/7/23 16:04
 */
public interface ITaskService {
    public Task createTask(CreateTaskParams params, MultipartFile file);

    public Page<Task> getPageList(ListPageParams params);

    public Task getTaskByTaskId(String taskId);

    public Task updateTaskByTaskId(TaskVo taskVo);

    @Transactional
    public void deleteTaskByTaskId(String taskId);

    public TaskVo createFromGit(CreateTaskParams params);

    public TaskVo createFromZip(CreateTaskParams params, MultipartFile file);
}
