package com.hxg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hxg.mapper.TaskMapper;
import com.hxg.model.entity.Task;
import com.hxg.model.enums.TaskStatusEnum;
import com.hxg.model.param.CreateTaskParams;
import com.hxg.model.param.ListPageParams;
import com.hxg.model.vo.TaskVo;
import com.hxg.service.ICatalogueService;
import com.hxg.service.IFileService;
import com.hxg.service.IGitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskServiceImpl 单元测试
 * 
 * @author hxg
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService 单元测试")
class TaskServiceImplTest {

    @Mock
    private TaskMapper taskMapper;
    
    @Mock
    private IGitService gitService;
    
    @Mock
    private IFileService fileService;
    
    @Mock
    private ICatalogueService catalogueService;
    
    @Mock
    private ThreadPoolTaskExecutor createTaskExecutor;

    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
        ReflectionTestUtils.setField(taskService, "gitService", gitService);
        ReflectionTestUtils.setField(taskService, "fileService", fileService);
        ReflectionTestUtils.setField(taskService, "catalogueService", catalogueService);
        ReflectionTestUtils.setField(taskService, "createTaskExecutor", createTaskExecutor);
    }

    @Test
    @DisplayName("从Git创建任务")
    void testCreateFromGit() {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName("codeLearn");
        params.setProjectUrl("https://github.com/hanxiaoge08/codeLearn.git");
        params.setUserName("testuser");

        
        String localPath = "/test/path";
        
        when(fileService.getRepositoryPath(anyString(), anyString())).thenReturn(localPath);
        when(gitService.cloneRepository(any(CreateTaskParams.class), anyString())).thenReturn(localPath);
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        
        // 模拟异步执行
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            CompletableFuture.runAsync(task);
            return null;
        }).when(createTaskExecutor).execute(any(Runnable.class));

        // When
        TaskVo result = taskService.createFromGit(params);

        // Then
        assertNotNull(result);
        assertEquals("codeLearn", result.getProjectName());
        assertEquals("https://github.com/hanxiaoge08/codeLearn.git", result.getProjectUrl());
        assertEquals(1, result.getStatus());
        
        // 验证服务调用
        verify(fileService).getRepositoryPath("testuser", "codeLearn");
        verify(gitService).cloneRepository(params, localPath);
        verify(taskMapper).insert(any(Task.class));
        verify(createTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("从ZIP文件创建任务")
    void testCreateFromZip() {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName("zip-project");
        params.setUserName("zipuser");
        
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.zip", "application/zip", "zip content".getBytes());
        
        String localPath = "/zip/path";
        
        when(fileService.getRepositoryPath(anyString(), anyString())).thenReturn(localPath);
        when(fileService.unzipToProjectDir(any(), anyString(), anyString())).thenReturn(localPath);
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            CompletableFuture.runAsync(task);
            return null;
        }).when(createTaskExecutor).execute(any(Runnable.class));

        // When
        TaskVo result = taskService.createFromZip(params, mockFile);

        // Then
        assertNotNull(result);
        assertEquals("zip-project", result.getProjectName());
        assertEquals("zipuser", result.getUserName());
        assertEquals(1, result.getStatus());
        
        // 验证服务调用
        verify(fileService).getRepositoryPath("zipuser", "zip-project");
        verify(fileService).unzipToProjectDir(mockFile, "zipuser", "zip-project");
        verify(taskMapper).insert(any(Task.class));
    }

    @Test
    @DisplayName("从ZIP文件创建任务失败")
    void testCreateFromZipFailure() {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName("fail-project");
        params.setUserName("failuser");
        
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.zip", "application/zip", "zip content".getBytes());
        
        when(fileService.getRepositoryPath(anyString(), anyString())).thenThrow(new RuntimeException("解压失败"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> taskService.createFromZip(params, mockFile));
        
        assertTrue(exception.getMessage().contains("处理ZIP文件失败"));
    }

    @Test
    @DisplayName("分页获取任务列表")
    void testGetPageList() {
        // Given
        ListPageParams params = new ListPageParams();
        params.setPageIndex(1);
        params.setPageSize(10);
        params.setTaskId("task123");
        params.setProjectName("test-project");
        params.setUserName("testuser");
        
        Page<Task> expectedPage = new Page<>(1, 10);
        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(expectedPage);

        // When
        Page<Task> result = taskService.getPageList(params);

        // Then
        assertEquals(expectedPage, result);
        
        // 验证查询参数
        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(taskMapper).selectPage(any(Page.class), captor.capture());
        
        // 这里可以进一步验证查询条件，但LambdaQueryWrapper的内部状态较难直接验证
    }

    @Test
    @DisplayName("根据任务ID获取任务")
    void testGetTaskByTaskId() {
        // Given
        String taskId = "TASK_1753790355262";
        Task expectedTask = new Task();
        expectedTask.setTaskId(taskId);
        expectedTask.setProjectName("CodeBase-CR");
        
        // MyBatis-Plus的getOne()方法会调用selectOne(queryWrapper, true)
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class), eq(true))).thenReturn(expectedTask);

        // When
        Task result = taskService.getTaskByTaskId(taskId);

        // Then
        assertEquals(expectedTask, result);
        verify(taskMapper).selectOne(any(LambdaQueryWrapper.class), eq(true));
    }

    @Test
    @DisplayName("更新任务")
    void testUpdateTaskByTaskId() {
        // Given
        TaskVo taskVo = new TaskVo();
        taskVo.setTaskId("task123");
        taskVo.setProjectName("updated-project");
        taskVo.setProjectUrl("https://github.com/updated/repo.git");
        taskVo.setUserName("updateduser");
        
        Task existingTask = new Task();
        existingTask.setTaskId("task123");
        existingTask.setProjectName("old-project");
        
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class), eq(true))).thenReturn(existingTask);
        when(taskMapper.updateById(any(Task.class))).thenReturn(1);

        // When
        Task result = taskService.updateTaskByTaskId(taskVo);

        // Then
        assertNotNull(result);
        assertEquals("updated-project", result.getProjectName());
        assertEquals("https://github.com/updated/repo.git", result.getProjectUrl());
        assertEquals("updateduser", result.getUserName());
        assertNotNull(result.getUpdateTime());
        
        verify(taskMapper).updateById(result);
    }

    @Test
    @DisplayName("删除任务")
    void testDeleteTaskByTaskId() {
        // Given
        String taskId = "task123";
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setTaskId(taskId);
        existingTask.setProjectName("delete-project");
        existingTask.setUserName("deleteuser");
        
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class), eq(true))).thenReturn(existingTask);
        when(taskMapper.deleteById(anyLong())).thenReturn(1);
        doNothing().when(fileService).deleteProjectDirectory(anyString(), anyString());
        doNothing().when(catalogueService).deleteCatalogueByTaskId(anyString());

        // When
        taskService.deleteTaskByTaskId(taskId);

        // Then
        verify(fileService).deleteProjectDirectory("deleteuser", "delete-project");
        verify(catalogueService).deleteCatalogueByTaskId(taskId);
        verify(taskMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除不存在的任务")
    void testDeleteNonExistentTask() {
        // Given
        String taskId = "nonexistent";
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class), eq(true))).thenReturn(null);

        // When
        taskService.deleteTaskByTaskId(taskId);

        // Then
        verify(fileService, never()).deleteProjectDirectory(anyString(), anyString());
        verify(catalogueService, never()).deleteCatalogueByTaskId(anyString());
        verify(taskMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除任务时文件服务失败")
    void testDeleteTaskWithFileServiceFailure() {
        // Given
        String taskId = "task123";
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setTaskId(taskId);
        existingTask.setProjectName("delete-project");
        existingTask.setUserName("deleteuser");
        
        when(taskMapper.selectOne(any(LambdaQueryWrapper.class), eq(true))).thenReturn(existingTask);
        when(taskMapper.deleteById(anyLong())).thenReturn(1);
        doThrow(new RuntimeException("删除文件失败")).when(fileService).deleteProjectDirectory(anyString(), anyString());
        doNothing().when(catalogueService).deleteCatalogueByTaskId(anyString());

        // When - 不应该抛出异常，应该继续删除数据库记录
        assertDoesNotThrow(() -> taskService.deleteTaskByTaskId(taskId));

        // Then
        verify(catalogueService).deleteCatalogueByTaskId(taskId);
        verify(taskMapper).deleteById(1L);
    }

    @Test
    @DisplayName("创建任务时插入数据库")
    void testInsertTask() {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectName("insert-test");
        params.setProjectUrl("https://github.com/test/insert.git");
        params.setUserName("insertuser");
        
        when(taskMapper.insert(any(Task.class))).thenReturn(1);
        
        // When
        Task result = taskService.createTask(params, null);

        // Then
        assertNotNull(result);
        assertEquals("insert-test", result.getProjectName());
        assertEquals("https://github.com/test/insert.git", result.getProjectUrl());
        assertEquals("insertuser", result.getUserName());
        assertEquals(TaskStatusEnum.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getTaskId());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        
        verify(taskMapper).insert(result);
    }
}