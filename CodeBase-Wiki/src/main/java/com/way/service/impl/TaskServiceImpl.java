package com.way.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.way.model.context.ExecutionContext;
import com.way.model.mapper.TaskMapper;
import com.way.model.dto.GenCatalogueDTO;
import com.way.model.entity.Task;
import com.way.model.enums.TaskStatusEnum;
import com.way.model.param.CreateTaskParams;
import com.way.model.param.ListPageParams;
import com.way.model.vo.TaskVo;
import com.way.service.ICatalogueService;
import com.way.service.IFileService;
import com.way.service.IGitService;
import com.way.service.ITaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.way.utils.TaskIdGenerator;

import java.time.LocalDateTime;

/**
 * @author way
 * @description: 任务服务实现类
 * @date 2025/7/23 16:05
 */
@Service
@Slf4j
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {
    @Resource(name = "CreateTaskExecutor")
    private ThreadPoolTaskExecutor createTaskExecutor;

    @Autowired
    private IGitService gitService;

    @Autowired
    private IFileService fileService;

    @Autowired
    private ICatalogueService catalogueService;

    @Override
    public Task createTask(CreateTaskParams params, MultipartFile file) {
        //根据项目来源处理本地目录
        String localPath = fileService.getRepositoryPath(params.getUserName(), params.getProjectName());

        if ("git".equals(params.getSourceType())) {
            log.info("开始从Git仓库拉取项目");
            gitService.cloneRepository(params, localPath);
            log.info("拉取项目成功");
        } else {
            log.info("开始解压ZIP文件");
            //解压到项目目录
            fileService.unzipToProjectDir(file, params.getUserName(), params.getProjectName());
            log.info("解压ZIP文件成功");
        }

        Task task = insertTask(params);

        ExecutionContext context = new ExecutionContext();
        context.setTask(task);
        context.setCreateParams(params);
        context.setTaskId(task.getTaskId());
        context.setLocalPath(localPath);

        //异步处理任务
        createTaskExecutor.execute(() -> {
            try {
                executeTask(context);
            } catch (Exception e) {
                log.error("任务{}执行失败：{}", task.getTaskId(), e.getMessage());
                task.setStatus(TaskStatusEnum.FAILED);
                task.setFailReason(e.getMessage());
                task.setUpdateTime(LocalDateTime.now());
                this.updateById(task);
            }
        });
        return task;
    }

    private void executeTask(ExecutionContext context) {
        Task task = context.getTask();
        try {
            //生成项目目录
            String fileTree = fileService.getFileTree(context.getLocalPath());
            GenCatalogueDTO catalogueDTO = catalogueService.generateCatalogue(fileTree, context);

            // 缓存项目路径到CatalogueService，避免循环依赖
            catalogueService.cacheTaskProjectPath(context.getTaskId(), context.getLocalPath());

            //生成目录详情 - 传递projectName
            String projectName = task.getProjectName();
            catalogueService.parallelGenerateCatalogueDetail(fileTree, catalogueDTO, context.getLocalPath(), projectName);
            task.setStatus(TaskStatusEnum.COMPLETED);
            task.setUpdateTime(LocalDateTime.now());
        } catch (Exception e) {
            log.error("任务执行失败", e);
            task.setStatus(TaskStatusEnum.FAILED);
            task.setFailReason(e.getMessage());
            task.setUpdateTime(LocalDateTime.now());
        } finally {
            this.updateById(task);
            // 清理CatalogueService中的缓存数据
            catalogueService.cleanupTaskCache(context.getTaskId());
        }
    }

    private Task insertTask(CreateTaskParams params) {
        Task task = Task.builder()
                .taskId(TaskIdGenerator.generate())
                .projectName(params.getProjectName())
                .projectUrl(params.getProjectUrl())
                .userName(params.getUserName())
                .status(TaskStatusEnum.IN_PROGRESS)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        this.save(task);
        return task;
    }

    @Override
    public Page<Task> getPageList(ListPageParams params) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        if (params.getTaskId() != null && !params.getTaskId().isEmpty()) {
            queryWrapper.eq(Task::getTaskId, params.getTaskId());
        }
        if (params.getProjectName() != null && !params.getProjectName().isEmpty()) {
            queryWrapper.eq(Task::getProjectName, params.getProjectName());
        }
        if (params.getUserName() != null && !params.getUserName().isEmpty()) {
            queryWrapper.eq(Task::getUserName, params.getUserName());
        }
        return this.page(new Page<>(params.getPageIndex(), params.getPageSize()), queryWrapper);
    }

    @Override
    public Task getTaskByTaskId(String taskId) {
        return this.getOne(new LambdaQueryWrapper<Task>().eq(Task::getTaskId, taskId));
    }

    @Override
    public Task updateTaskByTaskId(TaskVo taskVo) {
        Task task = getTaskByTaskId(taskVo.getTaskId());
        task.setProjectName(taskVo.getProjectName());
        task.setProjectUrl(taskVo.getProjectUrl());
        task.setUserName(taskVo.getUserName());
        task.setUpdateTime(LocalDateTime.now());
        this.updateById(task);
        return task;
    }

    @Override
    public void deleteTaskByTaskId(String taskId) {
        Task task = getTaskByTaskId(taskId);
        if (task != null) {
            //获取任务关联的项目信息
            String projectName = task.getProjectName();
            String userName = task.getUserName();
            try {
                //删除项目目录
                fileService.deleteProjectDirectory(userName, projectName);
                log.info("任务{}的项目目录 {} 删除成功", taskId, projectName);
            } catch (Exception e) {
                log.error("任务{}的项目目录 {} 删除失败: {}", taskId, projectName, e.getMessage());
            }

            //删除目录
            catalogueService.deleteCatalogueByTaskId(taskId);

            this.removeById(task.getId());
            log.info("任务{}删除成功", taskId);
        } else {
            log.info("任务{}不存在", taskId);
        }

    }

    @Override
    public TaskVo createFromGit(CreateTaskParams params) {
        params.setSourceType("git");
        Task task = createTask(params, null);
        return TaskVo.fromEntity(task);
    }

    @Override
    public TaskVo createFromZip(CreateTaskParams params, MultipartFile file) {
        try {
            Task task = createTask(params, file);
            return TaskVo.fromEntity(task);
        } catch (Exception e) {
            log.error("处理ZIP文件失败", e);
            throw new RuntimeException("处理ZIP文件失败:" + e.getMessage());
        }
    }


}
