package com.way.crApp.adapter.github;

import com.way.crApp.dto.review.ReviewCommentDTO;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * GitHub 适配器
 * 
 * 职责：封装所有对 GitHub API 的调用
 * 核心方法：getPullRequestDiff(...), publishLineComment(...), cloneRepository(...)
 * 内部使用 GitHubClientConfig 中配置好的客户端实例
 */
@Component
public class GitHubAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GitHubAdapter.class);

    @Autowired
    private GitHub github;

    /**
     * 获取Pull Request的diff内容
     * 
     * @param repoFullName 仓库全名 (owner/repo)
     * @param prNumber PR编号
     * @return diff内容
     */
    public String getPullRequestDiff(String repoFullName, Integer prNumber) {
        try {
            GHRepository repository = github.getRepository(repoFullName);
            GHPullRequest pullRequest = repository.getPullRequest(prNumber);
            
            // 获取diff URL并下载内容
            URL diffUrl = new URL(pullRequest.getDiffUrl().toString());
            
            try (Scanner scanner = new Scanner(diffUrl.openStream(), StandardCharsets.UTF_8)) {
                StringBuilder diffContent = new StringBuilder();
                while (scanner.hasNextLine()) {
                    diffContent.append(scanner.nextLine()).append("\n");
                }
                return diffContent.toString();
            }
            
        } catch (IOException e) {
            logger.error("获取PR diff时发生错误: repo={}, pr={}", repoFullName, prNumber, e);
            return null;
        }
    }

    /**
     * 发布行级评论到PR
     * 
     * @param repoFullName 仓库全名
     * @param prNumber PR编号
     * @param comment 评论信息
     */
    public void publishLineComment(String repoFullName, Integer prNumber, ReviewCommentDTO comment) {
        try {
            GHRepository repository = github.getRepository(repoFullName);
            GHPullRequest pullRequest = repository.getPullRequest(prNumber);
            
            // 获取PR的最新commit
            String latestCommitSha = pullRequest.getHead().getSha();
            
            // 创建行级评论
            pullRequest.createReviewComment(
                comment.comment(),
                latestCommitSha,
                comment.filePath(),
                comment.lineNumber()
            );
            
            logger.debug("已发布行级评论: file={}, line={}", comment.filePath(), comment.lineNumber());
            
        } catch (IOException e) {
            logger.error("发布行级评论时发生错误: repo={}, pr={}, file={}, line={}", 
                repoFullName, prNumber, comment.filePath(), comment.lineNumber(), e);
        }
    }

    /**
     * 发布总体评论到PR
     * 
     * @param repoFullName 仓库全名
     * @param prNumber PR编号
     * @param comment 评论内容
     */
    public void publishGeneralComment(String repoFullName, Integer prNumber, String comment) {
        try {
            GHRepository repository = github.getRepository(repoFullName);
            GHPullRequest pullRequest = repository.getPullRequest(prNumber);
            
            pullRequest.comment(comment);
            
            logger.debug("已发布总体评论到PR: repo={}, pr={}", repoFullName, prNumber);
            
        } catch (IOException e) {
            logger.error("发布总体评论时发生错误: repo={}, pr={}", repoFullName, prNumber, e);
        }
    }

    /**
     * 获取仓库信息
     * 
     * @param repoFullName 仓库全名
     * @return 仓库信息
     */
    public GHRepository getRepository(String repoFullName) {
        try {
            return github.getRepository(repoFullName);
        } catch (IOException e) {
            logger.error("获取仓库信息时发生错误: repo={}", repoFullName, e);
            return null;
        }
    }

    /**
     * 检查仓库是否存在且可访问
     * 
     * @param repoFullName 仓库全名
     * @return true如果可访问
     */
    public boolean isRepositoryAccessible(String repoFullName) {
        try {
            GHRepository repo = github.getRepository(repoFullName);
            return repo != null;
        } catch (IOException e) {
            logger.warn("仓库不可访问: repo={}, error={}", repoFullName, e.getMessage());
            return false;
        }
    }
} 