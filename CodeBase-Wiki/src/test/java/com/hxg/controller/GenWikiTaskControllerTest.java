package com.hxg.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.entity.Task;
import com.hxg.model.param.CreateTaskParams;
import com.hxg.model.param.ListPageParams;
import com.hxg.model.vo.CatalogueListVo;
import com.hxg.model.vo.TaskVo;
import com.hxg.service.ICatalogueService;
import com.hxg.service.ITaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GenWikiTaskController 单元测试
 * 
 * @author hxg
 */
@WebMvcTest(GenWikiTaskController.class)
@DisplayName("GenWikiTaskController 单元测试")
class GenWikiTaskControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ITaskService taskService() {
            return Mockito.mock(ITaskService.class);
        }

        @Bean
        @Primary
        public ICatalogueService catalogueService() {
            return Mockito.mock(ICatalogueService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private ICatalogueService catalogueService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("从Git创建任务")
    void testCreateFromGit() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName("test-project");
        params.setProjectUrl("https://github.com/test/repo.git");
        params.setUserName("testuser");
        params.setBranch("main");

        TaskVo expectedTask = new TaskVo();
        expectedTask.setTaskId("task123");
        expectedTask.setProjectName("test-project");
        expectedTask.setProjectUrl("https://github.com/test/repo.git");
        expectedTask.setUserName("testuser");

        when(taskService.createFromGit(any(CreateTaskParams.class))).thenReturn(expectedTask);

        // When & Then
        mockMvc.perform(post("/api/task/create/git")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.projectName").value("test-project"))
                .andExpect(jsonPath("$.data.projectUrl").value("https://github.com/test/repo.git"))
                .andExpect(jsonPath("$.data.userName").value("testuser"));

        verify(taskService).createFromGit(any(CreateTaskParams.class));
    }

    @Test
    @DisplayName("从ZIP文件创建任务")
    void testCreateFromZip() throws Exception {
        // Given
        MockMultipartFile zipFile = new MockMultipartFile(
            "file", "test.zip", "application/zip", "zip content".getBytes());
        
        String projectName = "zip-project";
        String userName = "zipuser";

        TaskVo expectedTask = new TaskVo();
        expectedTask.setTaskId("zip-task123");
        expectedTask.setProjectName(projectName);
        expectedTask.setUserName(userName);

        when(taskService.createFromZip(any(CreateTaskParams.class), any())).thenReturn(expectedTask);

        // When & Then
        mockMvc.perform(multipart("/api/task/create/zip")
                .file(zipFile)
                .param("projectName", projectName)
                .param("userName", userName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("zip-task123"))
                .andExpect(jsonPath("$.data.projectName").value(projectName))
                .andExpect(jsonPath("$.data.userName").value(userName));

        verify(taskService).createFromZip(any(CreateTaskParams.class), any());
    }

    @Test
    @DisplayName("分页获取任务列表")
    void testGetTasksByPage() throws Exception {
        // Given
        ListPageParams params = new ListPageParams();
        params.setPageIndex(1);
        params.setPageSize(10);
        params.setTaskId("task123");

        Page<Task> expectedPage = new Page<>(1, 10);
        Task task = new Task();
        task.setTaskId("task123");
        task.setProjectName("test-project");
        expectedPage.setRecords(Arrays.asList(task));
        expectedPage.setTotal(1);

        when(taskService.getPageList(any(ListPageParams.class))).thenReturn(expectedPage);

        // When & Then
        mockMvc.perform(post("/api/task/listPage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].taskId").value("task123"))
                .andExpect(jsonPath("$.data.records[0].projectName").value("test-project"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(taskService).getPageList(any(ListPageParams.class));
    }

    @Test
    @DisplayName("获取任务详情")
    void testGetTaskDetail() throws Exception {
        // Given
        String taskId = "task123";
        Task expectedTask = new Task();
        expectedTask.setTaskId(taskId);
        expectedTask.setProjectName("detail-project");
        expectedTask.setUserName("detailuser");

        when(taskService.getTaskByTaskId(taskId)).thenReturn(expectedTask);

        // When & Then
        mockMvc.perform(get("/api/task/detail")
                .param("taskId", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.projectName").value("detail-project"))
                .andExpect(jsonPath("$.data.userName").value("detailuser"));

        verify(taskService).getTaskByTaskId(taskId);
    }

    @Test
    @DisplayName("更新任务")
    void testUpdateTask() throws Exception {
        // Given
        TaskVo taskVo = new TaskVo();
        taskVo.setTaskId("task123");
        taskVo.setProjectName("updated-project");
        taskVo.setUserName("updateduser");

        Task updatedTask = new Task();
        updatedTask.setTaskId("task123");
        updatedTask.setProjectName("updated-project");
        updatedTask.setUserName("updateduser");

        when(taskService.updateTaskByTaskId(any(TaskVo.class))).thenReturn(updatedTask);

        // When & Then
        mockMvc.perform(put("/api/task/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskVo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value("task123"))
                .andExpect(jsonPath("$.data.projectName").value("updated-project"))
                .andExpect(jsonPath("$.data.userName").value("updateduser"));

        verify(taskService).updateTaskByTaskId(any(TaskVo.class));
    }

    @Test
    @DisplayName("删除任务")
    void testDeleteTask() throws Exception {
        // Given
        String taskId = "task123";
        doNothing().when(taskService).deleteTaskByTaskId(taskId);

        // When & Then
        mockMvc.perform(get("/api/task/delete")
                .param("taskId", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(taskService).deleteTaskByTaskId(taskId);
    }

    @Test
    @DisplayName("获取目录详情")
    void testGetCatalogueDetail() throws Exception {
        // Given
        String taskId = "task123";
        List<Catalogue> expectedCatalogues = Arrays.asList(
            createCatalogue("cat1", "目录1"),
            createCatalogue("cat2", "目录2")
        );

        when(catalogueService.getCatalogueByTaskId(taskId)).thenReturn(expectedCatalogues);

        // When & Then
        mockMvc.perform(get("/api/task/catalogue/detail")
                .param("taskId", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(catalogueService).getCatalogueByTaskId(taskId);
    }

    @Test
    @DisplayName("获取目录树")
    void testGetCatalogueTree() throws Exception {
        // Given
        String taskId = "task123";
        List<CatalogueListVo> expectedTree = Arrays.asList(
            createCatalogueListVo("tree1", "树节点1"),
            createCatalogueListVo("tree2", "树节点2")
        );

        when(catalogueService.getCatalogueTreeByTaskId(taskId)).thenReturn(expectedTree);

        // When & Then
        mockMvc.perform(get("/api/task/catalogue/tree")
                .param("taskId", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(catalogueService).getCatalogueTreeByTaskId(taskId);
    }

    @Test
    @DisplayName("创建任务失败时返回错误")
    void testCreateFromGitFailure() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName("fail-project");
        params.setProjectUrl("invalid-url");

        when(taskService.createFromGit(any(CreateTaskParams.class)))
            .thenThrow(new RuntimeException("创建任务失败"));

        // When & Then
        mockMvc.perform(post("/api/task/create/git")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isInternalServerError());

        verify(taskService).createFromGit(any(CreateTaskParams.class));
    }

    @Test
    @DisplayName("获取不存在的任务详情")
    void testGetNonExistentTaskDetail() throws Exception {
        // Given
        String taskId = "nonexistent";
        when(taskService.getTaskByTaskId(taskId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/task/detail")
                .param("taskId", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(taskService).getTaskByTaskId(taskId);
    }

    @Test
    @DisplayName("上传无效ZIP文件")
    void testCreateFromZipWithInvalidFile() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "not a zip file".getBytes());

        when(taskService.createFromZip(any(CreateTaskParams.class), any()))
            .thenThrow(new RuntimeException("处理ZIP文件失败"));

        // When & Then
        mockMvc.perform(multipart("/api/task/create/zip")
                .file(invalidFile)
                .param("projectName", "test-project")
                .param("userName", "testuser"))
                .andExpect(status().isInternalServerError());

        verify(taskService).createFromZip(any(CreateTaskParams.class), any());
    }

    // 辅助方法
    private Catalogue createCatalogue(String id, String name) {
        return Catalogue.builder()
            .catalogueId(id)
            .name(name)
            .title(name)
            .build();
    }

    private CatalogueListVo createCatalogueListVo(String id, String name) {
        return CatalogueListVo.builder()
            .catalogueId(id)
            .name(name)
            .title(name)
            .build();
    }
}