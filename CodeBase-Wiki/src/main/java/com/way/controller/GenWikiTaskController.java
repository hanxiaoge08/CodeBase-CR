package com.way.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.way.model.entity.Catalogue;
import com.way.model.entity.Task;
import com.way.model.param.CreateTaskParams;
import com.way.model.param.ListPageParams;
import com.way.model.vo.CatalogueListVo;
import com.way.model.vo.ResponseVo;
import com.way.model.vo.ResultVo;
import com.way.model.vo.TaskVo;
import com.way.service.ICatalogueService;
import com.way.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author way
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
        try{
            return ResultVo.success(taskService.createFromZip(params,file));
        }catch (RuntimeException e){
            log.error("从ZIP文件创建任务失败：{}",e.getMessage());
            return ResultVo.error(e.getMessage());
        }
        
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
