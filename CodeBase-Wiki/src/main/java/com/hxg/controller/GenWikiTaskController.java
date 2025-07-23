package com.hxg.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.entity.Task;
import com.hxg.model.param.CreateTaskParams;
import com.hxg.model.param.ListPageParams;
import com.hxg.model.vo.CatalogueListVo;
import com.hxg.model.vo.ResponseVo;
import com.hxg.model.vo.ResultVo;
import com.hxg.model.vo.TaskVo;
import com.hxg.service.ICatalogueService;
import com.hxg.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author hxg
 * @description: wiki相关接口
 * @date 2025/7/20 18:47
 */

@Slf4j
@RestController
@RequestMapping("/api/task")
public class GenWikiTaskController {

    @Autowired
    private ITaskService taskService;

    @Autowired
    private ICatalogueService catalogueService;

    @PostMapping("/create/git")
    public ResultVo<TaskVo> createFromGit(@RequestBody CreateTaskParams params) {
        return ResultVo.success(taskService.createFromGit(params));
    }

    @PostMapping("/create/zip")
    public ResultVo<TaskVo> createFromZip(
            @RequestPart("file") MultipartFile file,
            @RequestParam("projectName") String projectName,
            @RequestParam("userName") String userName) {
        log.info("接收到ZIP文件上传请求，文件名：{}，大小：{}bytes，项目名：{}，用户名：{}",
                file.getOriginalFilename(),file.getSize(),projectName,userName);
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName(projectName);
        params.setUserName(userName);
        params.setSourceType("zip");
        return ResultVo.success(taskService.createFromZip(params,file));
    }

    @PostMapping("/listPage")
    public ResponseVo<Page<Task>> getTasksByPage(@RequestBody ListPageParams params) {
        Page<Task> page = taskService.getPageList(params);
        return ResponseVo.success(page);
    }

    @GetMapping("/detail")
    public ResponseVo<Task> getTaskByTaskId(@RequestParam("taskId") String taskId) {
        return ResponseVo.success(taskService.getTaskByTaskId(taskId));
    }

    @PutMapping("/update")
    public ResponseVo<Task> updateTask(@RequestBody TaskVo task) {
        return ResponseVo.success(taskService.updateTaskByTaskId(task));
    }

    @GetMapping("/delete")
    public ResponseVo<Void> deleteTask(@RequestParam("taskId") String taskId) {
        taskService.deleteTaskByTaskId(taskId);
        return ResponseVo.success();
    }

    @GetMapping("/catalogue/detail")
    public ResponseVo<List<Catalogue>> getCatalogueDetail(@RequestParam("taskId") String taskId) {
        return ResponseVo.success(catalogueService.getCatalogueByTaskId(taskId));
    }

    @GetMapping("/catalogue/tree")
    public ResponseVo<List<CatalogueListVo>> getCatalogueTree(@RequestParam("taskId") String taskId) {
        return ResponseVo.success(catalogueService.getCatalogueTreeByTaskId(taskId));
    }
}
