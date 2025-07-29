package com.hxg.service.impl;

import com.hxg.service.IFileService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileServiceImpl 单元测试
 * 
 * @author hxg
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 单元测试")
class FileServiceImplTest {

    private FileServiceImpl fileService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileServiceImpl();
        // 设置测试用的repository路径
        ReflectionTestUtils.setField(fileService, "repositoryBasePath", tempDir.toString());
    }

    @Test
    @DisplayName("获取文件树 - 包含.gitignore过滤")
    void testGetFileTreeWithGitignore() throws IOException {
        // Given - 创建测试文件结构
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir);
        
        // 创建.gitignore文件
        Files.write(projectDir.resolve(".gitignore"), "*.log\ntarget/\n.idea/".getBytes());
        
        // 创建一些测试文件
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.write(projectDir.resolve("src/main/java/Test.java"), "public class Test {}".getBytes());
        Files.write(projectDir.resolve("README.md"), "# Test Project".getBytes());
        Files.write(projectDir.resolve("app.log"), "log content".getBytes()); // 应该被忽略
        Files.createDirectories(projectDir.resolve("target"));
        Files.write(projectDir.resolve("target/app.jar"), "jar content".getBytes()); // 应该被忽略

        // When
        String result = fileService.getFileTree(projectDir.toString());

        // Then
        assertNotNull(result);
        assertTrue(result.contains("README.md"));
        assertTrue(result.contains("src/main/java/Test.java"));
        assertFalse(result.contains("app.log")); // 被.gitignore忽略
        assertFalse(result.contains("target")); // 被.gitignore忽略
    }

    @Test
    @DisplayName("获取文件树 - 没有.gitignore文件")
    void testGetFileTreeWithoutGitignore() throws IOException {
        // Given
        Path projectDir = tempDir.resolve("no-gitignore-project");
        Files.createDirectories(projectDir);
        
        Files.write(projectDir.resolve("README.md"), "# Test".getBytes());
        Files.write(projectDir.resolve("app.log"), "log".getBytes());

        // When
        String result = fileService.getFileTree(projectDir.toString());

        // Then
        assertNotNull(result);
        assertTrue(result.contains("README.md"));
        assertTrue(result.contains("app.log")); // 没有.gitignore，所有文件都包含
    }

    @Test
    @DisplayName("解压ZIP文件到项目目录")
    void testUnzipToProjectDir() throws IOException {
        // Given - 创建测试ZIP文件
        byte[] zipContent = createTestZipFile();
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.zip", "application/zip", zipContent);
        
        String userName = "testuser";
        String projectName = "testproject";

        // When
        String result = fileService.unzipToProjectDir(mockFile, userName, projectName);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(userName));
        assertTrue(result.contains(projectName));
        
        // 验证文件是否被正确解压
        File resultDir = new File(result);
        assertTrue(resultDir.exists());
        assertTrue(new File(resultDir, "test.txt").exists());
        assertTrue(new File(resultDir, "subdir").exists());
        assertTrue(new File(resultDir, "subdir/nested.txt").exists());
    }

    @Test
    @DisplayName("解压ZIP文件失败时抛出异常")
    void testUnzipToProjectDirWithInvalidZip() {
        // Given - 无效的ZIP内容
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "invalid.zip", "application/zip", "invalid zip content".getBytes());

        // When & Then
        assertThrows(RuntimeException.class, 
            () -> fileService.unzipToProjectDir(mockFile, "user", "project"));
    }

    @Test
    @DisplayName("获取仓库路径 - 创建新目录")
    void testGetRepositoryPathCreateNew() {
        // Given
        String userName = "newuser";
        String projectName = "newproject";

        // When
        String result = fileService.getRepositoryPath(userName, projectName);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(userName));
        assertTrue(result.contains(projectName));
        
        File projectDir = new File(result);
        assertTrue(projectDir.exists());
        assertTrue(projectDir.isDirectory());
    }

    @Test
    @DisplayName("获取仓库路径 - 删除已存在的目录")
    void testGetRepositoryPathDeleteExisting() throws IOException {
        // Given - 先创建一个已存在的项目目录
        String userName = "existinguser";
        String projectName = "existingproject";
        
        String firstResult = fileService.getRepositoryPath(userName, projectName);
        File existingDir = new File(firstResult);
        File testFile = new File(existingDir, "existing.txt");
        Files.write(testFile.toPath(), "existing content".getBytes());
        assertTrue(testFile.exists());

        // When - 再次获取路径（应该删除旧目录并创建新的）
        String result = fileService.getRepositoryPath(userName, projectName);

        // Then
        assertEquals(firstResult, result);
        File projectDir = new File(result);
        assertTrue(projectDir.exists());
        assertFalse(testFile.exists()); // 旧文件应该被删除
    }

    @Test
    @DisplayName("删除项目目录")
    void testDeleteProjectDirectory() throws IOException {
        // Given - 创建一个项目目录
        String userName = "deleteuser";
        String projectName = "deleteproject";
        
        String projectPath = fileService.getRepositoryPath(userName, projectName);
        File projectDir = new File(projectPath);
        File testFile = new File(projectDir, "test.txt");
        Files.write(testFile.toPath(), "test content".getBytes());
        assertTrue(testFile.exists());

        // When
        fileService.deleteProjectDirectory(userName, projectName);

        // Then
        assertFalse(projectDir.exists());
    }

    @Test
    @DisplayName("删除不存在的项目目录")
    void testDeleteNonExistentProjectDirectory() {
        // Given
        String userName = "nonexistentuser";
        String projectName = "nonexistentproject";

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> fileService.deleteProjectDirectory(userName, projectName));
    }

    @Test
    @DisplayName("删除项目目录时用户名为空")
    void testDeleteProjectDirectoryWithNullUserName() {
        // When & Then
        assertDoesNotThrow(() -> fileService.deleteProjectDirectory(null, "project"));
        assertDoesNotThrow(() -> fileService.deleteProjectDirectory("user", null));
    }

    @Test
    @DisplayName("测试相对路径配置")
    void testRelativePathConfiguration() {
        // Given
        FileServiceImpl service = new FileServiceImpl();
        ReflectionTestUtils.setField(service, "repositoryBasePath", "./relative-repo");
        
        // When
        String result = service.getRepositoryPath("user", "project");
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("relative-repo"));
        assertTrue(result.contains("user"));
        assertTrue(result.contains("project"));
    }

    @Test
    @DisplayName("测试绝对路径配置")
    void testAbsolutePathConfiguration() {
        // Given
        FileServiceImpl service = new FileServiceImpl();
        String absolutePath = tempDir.toAbsolutePath().toString();
        ReflectionTestUtils.setField(service, "repositoryBasePath", absolutePath);
        
        // When
        String result = service.getRepositoryPath("user", "project");
        
        // Then
        assertNotNull(result);
        assertTrue(result.startsWith(absolutePath));
        assertTrue(result.contains("user"));
        assertTrue(result.contains("project"));
    }

    /**
     * 创建测试用的ZIP文件内容
     */
    private byte[] createTestZipFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 添加文件
            ZipEntry entry1 = new ZipEntry("test.txt");
            zos.putNextEntry(entry1);
            zos.write("test content".getBytes());
            zos.closeEntry();

            // 添加目录
            ZipEntry dirEntry = new ZipEntry("subdir/");
            zos.putNextEntry(dirEntry);
            zos.closeEntry();

            // 添加目录中的文件
            ZipEntry entry2 = new ZipEntry("subdir/nested.txt");
            zos.putNextEntry(entry2);
            zos.write("nested content".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}