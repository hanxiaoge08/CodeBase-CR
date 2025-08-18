package com.way.crApp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.way.crApp.dto.github.PullRequestEventDTO;
import com.way.crApp.dto.review.ReviewTaskDTO;
import com.way.crApp.service.port.ICodeReviewService;
import com.way.crApp.service.port.IGithubWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * GitHub Webhook 服务实现
 * 
 * 解析JSON，创建一个 ReviewTaskDTO，并异步调用代码审查服务
 */
@Service
public class GithubWebhookServiceImpl implements IGithubWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(GithubWebhookServiceImpl.class);

    @Autowired
    private ICodeReviewService codeReviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Async("reviewTaskExecutor")
    public void handlePullRequestEvent(String payload) {
        try {
            // 解析JSON负载
            PullRequestEventDTO event = objectMapper.readValue(payload, PullRequestEventDTO.class);
            
            // 检查是否是我们关心的事件
            if (!shouldProcessEvent(event)) {
                logger.info("跳过处理事件: action={}, state={}",
                    event.action(), event.pullRequest().state());
                return;
            }
            
            // 构建审查任务
            ReviewTaskDTO task = buildReviewTask(event);
            
            logger.info("开始处理PR审查任务: repo={}, pr={}, title={}", 
                task.repositoryFullName(), task.prNumber(), task.prTitle());
            
            // 异步执行审查
            codeReviewService.performReview(task);
            
        } catch (Exception e) {
            logger.error("处理Pull Request事件时发生错误", e);
        }
    }

    /**
     * 判断是否应该处理此事件
     * 
     * @param event PR事件
     * @return true如果应该处理
     */
    private boolean shouldProcessEvent(PullRequestEventDTO event) {
        // 只处理opened和synchronize事件
        String action = event.action();
        if (!"opened".equals(action) && !"synchronize".equals(action)&&!"reopened".equals(action)) {
            return false;
        }
        
        // 只处理open状态的PR
        return "open".equals(event.pullRequest().state());
    }

    /**
     * 构建审查任务DTO
     * 
     * @param event PR事件
     * @return 审查任务
     */
    private ReviewTaskDTO buildReviewTask(PullRequestEventDTO event) {
        return ReviewTaskDTO.builder()
            .repositoryFullName(event.repository().fullName())
            .prNumber(event.pullRequest().number())
            .diffUrl(event.pullRequest().diffUrl())
            .prTitle(event.pullRequest().title())
            .prAuthor(event.pullRequest().user().login())
            .headSha(event.pullRequest().head().sha())
            .baseSha(event.pullRequest().base().sha())
            .cloneUrl(event.repository().cloneUrl())
            .build();
    }
} 