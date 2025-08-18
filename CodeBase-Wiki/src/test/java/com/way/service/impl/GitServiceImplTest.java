package com.way.service.impl;

import com.way.model.param.CreateTaskParams;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GitServiceImpl 单元测试
 * 
 * @author way
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitService 单元测试")
class GitServiceImplTest {

    @Mock
    private CloneCommand mockCloneCommand;
    
    @Mock
    private Git mockGit;

    private GitServiceImpl gitService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitService = new GitServiceImpl();
    }

    @Test
    @DisplayName("成功克隆公开仓库")
    void testCloneRepositorySuccess() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl("https://github.com/test/repo.git");
        params.setBranch("main");
        
        String localPath = tempDir.resolve("test-repo").toString();

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setBranch(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            doNothing().when(mockGit).close();

            // When
            String result = gitService.cloneRepository(params, localPath);

            // Then
            assertEquals(localPath, result);
            
            // 验证调用了正确的方法
            gitMock.verify(Git::cloneRepository, times(1));
            verify(mockCloneCommand).setURI("https://github.com/test/repo.git");
            verify(mockCloneCommand).setDirectory(new File(localPath));
            verify(mockCloneCommand).setBranch("main");
            verify(mockCloneCommand).call();
            verify(mockGit).close();
        }
    }

    @Test
    @DisplayName("成功克隆需要认证的私有仓库")
    void testClonePrivateRepositoryWithCredentials() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl("https://github.com/private/repo.git");
        params.setBranch("develop");
        params.setUserName("testuser");
        params.setPassword("testpass");
        
        String localPath = tempDir.resolve("private-repo").toString();

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setBranch(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setCredentialsProvider(any(CredentialsProvider.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            doNothing().when(mockGit).close();

            // When
            String result = gitService.cloneRepository(params, localPath);

            // Then
            assertEquals(localPath, result);
            
            // 验证设置了认证信息
            verify(mockCloneCommand).setCredentialsProvider(any(UsernamePasswordCredentialsProvider.class));
            verify(mockCloneCommand).call();
        }
    }

    @Test
    @DisplayName("克隆仓库时不指定分支，使用默认分支")
    void testCloneRepositoryWithoutBranch() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl("https://github.com/test/repo.git");
        // 不设置分支
        
        String localPath = tempDir.resolve("default-branch-repo").toString();

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            doNothing().when(mockGit).close();

            // When
            String result = gitService.cloneRepository(params, localPath);

            // Then
            assertEquals(localPath, result);
            
            // 验证没有调用setBranch方法
            verify(mockCloneCommand, never()).setBranch(anyString());
        }
    }

    @Test
    @DisplayName("克隆仓库时用户名为空，不设置认证")
    void testCloneRepositoryWithoutCredentials() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl("https://github.com/test/repo.git");
        params.setUserName(""); // 空用户名
        params.setPassword("password");
        
        String localPath = tempDir.resolve("no-auth-repo").toString();

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            doNothing().when(mockGit).close();

            // When
            String result = gitService.cloneRepository(params, localPath);

            // Then
            assertEquals(localPath, result);
            
            // 验证没有设置认证
            verify(mockCloneCommand, never()).setCredentialsProvider(any(CredentialsProvider.class));
        }
    }

    @Test
    @DisplayName("克隆仓库失败时抛出运行时异常")
    void testCloneRepositoryFailure() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl("https://github.com/invalid/repo.git");
        
        String localPath = tempDir.resolve("failed-repo").toString();
        
        GitAPIException gitException = new GitAPIException("Repository not found") {};

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenThrow(gitException);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> gitService.cloneRepository(params, localPath));
            
            assertTrue(exception.getMessage().contains("克隆仓库失败"));
            assertTrue(exception.getMessage().contains("Repository not found"));
            assertEquals(gitException, exception.getCause());
        }
    }

    @Test
    @DisplayName("参数验证 - projectUrl为空")
    void testCloneRepositoryWithNullUrl() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl(null);
        
        String localPath = tempDir.resolve("null-url-repo").toString();

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(isNull())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            doNothing().when(mockGit).close();

            // When
            String result = gitService.cloneRepository(params, localPath);

            // Then
            assertEquals(localPath, result);
            verify(mockCloneCommand).setURI(null);
        }
    }

    @Test
    @DisplayName("测试分支名为空字符串时的处理")
    void testCloneRepositoryWithEmptyBranch() throws Exception {
        // Given
        CreateTaskParams params = new CreateTaskParams();
        params.setProjectUrl("https://github.com/test/repo.git");
        params.setBranch(""); // 空字符串分支
        
        String localPath = tempDir.resolve("empty-branch-repo").toString();

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            doNothing().when(mockGit).close();

            // When
            String result = gitService.cloneRepository(params, localPath);

            // Then
            assertEquals(localPath, result);
            
            // 验证空字符串分支时不调用setBranch
            verify(mockCloneCommand, never()).setBranch(anyString());
        }
    }
}