package com.hxg.service.impl;

import com.hxg.queue.model.MemoryIndexTask;
import com.hxg.queue.producer.MemoryIndexProducer;
import com.hxg.model.entity.Catalogue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MemoryIntegrationServiceImpl 单元测试
 * 
 * @author hxg
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemoryIntegrationService 单元测试")
class MemoryIntegrationServiceImplTest {

    @Mock
    private MemoryIndexProducer memoryIndexProducer;

    private MemoryIntegrationServiceImpl memoryIntegrationService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        memoryIntegrationService = new MemoryIntegrationServiceImpl();
        ReflectionTestUtils.setField(memoryIntegrationService, "memoryIndexProducer", memoryIndexProducer);
    }

    @Test
    @DisplayName("异步索引项目到记忆")
    void testIndexProjectToMemoryAsync() throws Exception {
        // Given
        String taskId = "task123";
        List<Catalogue> catalogues = Arrays.asList(
            createCatalogue("cat1", "目录1", "# 目录1内容"),
            createCatalogue("cat2", "目录2", "# 目录2内容")
        );
        
        // 创建测试项目目录和代码文件
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir);
        Files.write(projectDir.resolve("Test.java"), "public class Test {}".getBytes());
        
        doNothing().when(memoryIndexProducer).sendDocumentTask(any(MemoryIndexTask.class));
        doNothing().when(memoryIndexProducer).sendCodeFileTask(any(MemoryIndexTask.class));

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexProjectToMemoryAsync(
            taskId, catalogues, projectDir.toString());

        // Then
        assertDoesNotThrow(() -> future.get());
        
        verify(memoryIndexProducer, atLeast(2)).sendDocumentTask(any(MemoryIndexTask.class));
        verify(memoryIndexProducer, atLeastOnce()).sendCodeFileTask(any(MemoryIndexTask.class));
    }

    @Test
    @DisplayName("异步索引单个文档到记忆")
    void testIndexDocumentToMemoryAsync() throws Exception {
        // Given
        String taskId = "task123";
        Catalogue catalogue = createCatalogue("doc1", "文档1", "# 文档1内容");

        doNothing().when(memoryIndexProducer).sendDocumentTask(any(MemoryIndexTask.class));

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexDocumentToMemoryAsync(taskId, catalogue);

        // Then
        assertDoesNotThrow(() -> future.get());
        
        verify(memoryIndexProducer).sendDocumentTask(any(MemoryIndexTask.class));
    }

    @Test
    @DisplayName("异步索引空内容文档时跳过")
    void testIndexDocumentToMemoryAsyncWithEmptyContent() throws Exception {
        // Given
        String taskId = "task123";
        Catalogue catalogue = createCatalogue("empty", "空文档", "");

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexDocumentToMemoryAsync(taskId, catalogue);

        // Then
        assertDoesNotThrow(() -> future.get());
        
        // 验证没有发送文档任务
        verify(memoryIndexProducer, never()).sendDocumentTask(any(MemoryIndexTask.class));
    }

    @Test
    @DisplayName("异步索引代码文件到记忆")
    void testIndexCodeFilesToMemoryAsync() throws Exception {
        // Given
        String taskId = "task123";
        
        // 创建测试代码文件
        Path projectDir = tempDir.resolve("code-project");
        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.write(projectDir.resolve("src/main/java/Test.java"), "public class Test {}".getBytes());
        Files.write(projectDir.resolve("src/main/java/Service.java"), "public class Service {}".getBytes());
        Files.write(projectDir.resolve("README.md"), "# Project".getBytes()); // 非代码文件，应该被忽略

        doNothing().when(memoryIndexProducer).sendCodeFileTask(any(MemoryIndexTask.class));

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexCodeFilesToMemoryAsync(
            taskId, projectDir.toString());

        // Then
        assertDoesNotThrow(() -> future.get());
        
        // 验证只调用了Java文件，不包含README.md
        verify(memoryIndexProducer, times(2)).sendCodeFileTask(any(MemoryIndexTask.class));
    }

    // Kafka 模式下 isMemoryServiceAvailable 恒为 true，无需健康检查测试

    @Test
    @DisplayName("索引项目时服务不可用")
    void testIndexProjectToMemoryAsyncServiceUnavailable() throws Exception {
        // Given
        String taskId = "task123";
        List<Catalogue> catalogues = Arrays.asList(
            createCatalogue("cat1", "目录1", "# 目录1内容")
        );
        
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir);

        // Kafka 模式下，无需模拟 Memory 服务不可用

        // When & Then - 不应该抛出异常，由fallback处理
        CompletableFuture<Void> future = memoryIntegrationService.indexProjectToMemoryAsync(
            taskId, catalogues, projectDir.toString());
        
        assertDoesNotThrow(() -> future.get());
        
        verify(memoryIndexProducer, atLeastOnce()).sendDocumentTask(any(MemoryIndexTask.class));
    }

    @Test
    @DisplayName("索引不存在的项目路径")
    void testIndexCodeFilesToMemoryAsyncNonExistentPath() throws Exception {
        // Given
        String taskId = "task123";
        String nonExistentPath = "/path/that/does/not/exist";

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexCodeFilesToMemoryAsync(
            taskId, nonExistentPath);

        // Then
        assertDoesNotThrow(() -> future.get());
        
        // 验证没有发送代码文件任务
        verify(memoryIndexProducer, never()).sendCodeFileTask(any(MemoryIndexTask.class));
    }

    // 辅助方法
    private Catalogue createCatalogue(String id, String name, String content) {
        return Catalogue.builder()
            .catalogueId(id)
            .name(name)
            .title(name)
            .content(content)
            .prompt("测试提示")
            .build();
    }
}