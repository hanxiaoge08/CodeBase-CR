package com.hxg.service.impl;

import com.hxg.memory.MemoryServiceClient;
import com.hxg.model.dto.BatchDocumentRequest;
import com.hxg.model.dto.CodeFileRequest;
import com.hxg.model.dto.DocumentRequest;
import com.hxg.model.entity.Catalogue;
import com.hxg.service.IMemoryIntegrationService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private MemoryServiceClient memoryServiceClient;

    private MemoryIntegrationServiceImpl memoryIntegrationService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        memoryIntegrationService = new MemoryIntegrationServiceImpl();
        ReflectionTestUtils.setField(memoryIntegrationService, "memoryServiceClient", memoryServiceClient);
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
        
        doNothing().when(memoryServiceClient).batchAddDocuments(any(BatchDocumentRequest.class));
        doNothing().when(memoryServiceClient).addCodeFile(any(CodeFileRequest.class));

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexProjectToMemoryAsync(
            taskId, catalogues, projectDir.toString());

        // Then
        assertDoesNotThrow(() -> future.get());
        
        verify(memoryServiceClient).batchAddDocuments(argThat(request -> 
            taskId.equals(request.getRepositoryId()) &&
            request.getDocuments().size() == 2
        ));
        verify(memoryServiceClient, atLeastOnce()).addCodeFile(any(CodeFileRequest.class));
    }

    @Test
    @DisplayName("异步索引单个文档到记忆")
    void testIndexDocumentToMemoryAsync() throws Exception {
        // Given
        String taskId = "task123";
        Catalogue catalogue = createCatalogue("doc1", "文档1", "# 文档1内容");

        doNothing().when(memoryServiceClient).addDocument(any(DocumentRequest.class));

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexDocumentToMemoryAsync(taskId, catalogue);

        // Then
        assertDoesNotThrow(() -> future.get());
        
        verify(memoryServiceClient).addDocument(argThat(request -> 
            taskId.equals(request.getRepositoryId()) &&
            "文档1".equals(request.getDocumentName()) &&
            "# 文档1内容".equals(request.getDocumentContent())
        ));
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
        
        // 验证没有调用addDocument
        verify(memoryServiceClient, never()).addDocument(any(DocumentRequest.class));
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

        doNothing().when(memoryServiceClient).addCodeFile(any(CodeFileRequest.class));

        // When
        CompletableFuture<Void> future = memoryIntegrationService.indexCodeFilesToMemoryAsync(
            taskId, projectDir.toString());

        // Then
        assertDoesNotThrow(() -> future.get());
        
        // 验证只调用了Java文件，不包含README.md
        verify(memoryServiceClient, times(2)).addCodeFile(any(CodeFileRequest.class));
    }

    @Test
    @DisplayName("检查记忆服务是否可用 - 服务正常")
    void testIsMemoryServiceAvailableTrue() {
        // Given
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "UP");
        healthResponse.put("service", "Memory Service");
        when(memoryServiceClient.healthCheck()).thenReturn(healthResponse);

        // When
        boolean result = memoryIntegrationService.isMemoryServiceAvailable();

        // Then
        assertTrue(result);
        verify(memoryServiceClient).healthCheck();
    }

    @Test
    @DisplayName("检查记忆服务是否可用 - 服务不可用")
    void testIsMemoryServiceAvailableFalse() {
        // Given
        Map<String, Object> healthResponse = new HashMap<>();
        healthResponse.put("status", "DOWN");
        healthResponse.put("message", "Memory Service Unavailable");
        when(memoryServiceClient.healthCheck()).thenReturn(healthResponse);

        // When
        boolean result = memoryIntegrationService.isMemoryServiceAvailable();

        // Then
        assertFalse(result);
        verify(memoryServiceClient).healthCheck();
    }

    @Test
    @DisplayName("检查记忆服务是否可用 - 异常情况")
    void testIsMemoryServiceAvailableException() {
        // Given
        when(memoryServiceClient.healthCheck()).thenThrow(FeignException.ServiceUnavailable.class);

        // When
        boolean result = memoryIntegrationService.isMemoryServiceAvailable();

        // Then
        assertFalse(result);
        verify(memoryServiceClient).healthCheck();
    }

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

        doThrow(FeignException.ServiceUnavailable.class).when(memoryServiceClient)
            .batchAddDocuments(any(BatchDocumentRequest.class));

        // When & Then - 不应该抛出异常，由fallback处理
        CompletableFuture<Void> future = memoryIntegrationService.indexProjectToMemoryAsync(
            taskId, catalogues, projectDir.toString());
        
        assertDoesNotThrow(() -> future.get());
        
        verify(memoryServiceClient).batchAddDocuments(any(BatchDocumentRequest.class));
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
        
        // 验证没有调用addCodeFile
        verify(memoryServiceClient, never()).addCodeFile(any(CodeFileRequest.class));
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