package com.hxg.github.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxg.github.dto.PullRequestEventDTO;
import com.hxg.github.dto.ReviewTaskDTO;
import com.hxg.github.service.IGithubWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * GitHub Webhook 服务实现
 * 
 * 解析JSON，创建一个 ReviewTaskDTO
 */
@Service
public class GithubWebhookServiceImpl implements IGithubWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(GithubWebhookServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Async
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
            
            logger.info("处理PR事件: repo={}, pr={}, title={}", 
                task.repositoryFullName(), task.prNumber(), task.prTitle());
            
            // TODO: 在这里可以触发具体的代码审查逻辑
            // 例如：发布到消息队列、调用外部服务等
            processReviewTask(task);
            
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
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
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

    /**
     * 处理审查任务
     * 
     * @param task 审查任务
     */
    private void processReviewTask(ReviewTaskDTO task) {
        // 这里可以添加具体的处理逻辑
        // 例如：
        // 1. 发布到消息队列
        // 2. 调用代码审查服务
        // 3. 存储到数据库等
        
        logger.info("开始处理审查任务: {}", task.repositoryFullName());
        
        // 示例：打印任务详情
        logger.debug("任务详情 - 仓库: {}, PR: {}, 作者: {}", 
            task.repositoryFullName(), task.prNumber(), task.prAuthor());
    }
} 