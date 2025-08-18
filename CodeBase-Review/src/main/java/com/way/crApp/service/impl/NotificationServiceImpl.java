package com.way.crApp.service.impl;

import com.way.crApp.dto.review.ReviewResultDTO;
import com.way.crApp.dto.review.ReviewTaskDTO;
import com.way.crApp.service.notification.NotificationChannel;
import com.way.crApp.service.notification.NotificationChannelFactory;
import com.way.crApp.service.port.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服务实现类
 * 支持多渠道异步通知发送
 */
@Service
public class NotificationServiceImpl implements INotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationChannelFactory channelFactory;

    public NotificationServiceImpl(NotificationChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    @Override
    @Async("notificationExecutor")
    public void sendReviewCompletionNotification(ReviewTaskDTO task, ReviewResultDTO result) {
        logger.info("开始发送代码评审完成通知: repo={}, pr={}", 
            task.repositoryFullName(), task.prNumber());

        List<NotificationChannel> enabledChannels = channelFactory.getEnabledChannels();
        
        if (enabledChannels.isEmpty()) {
            logger.debug("没有启用的通知渠道，跳过通知发送");
            return;
        }

        // 并行发送通知到所有启用的渠道
        List<CompletableFuture<Void>> futures = enabledChannels.stream()
            .map(channel -> CompletableFuture.runAsync(() -> {
                try {
                    logger.debug("通过 {} 渠道发送代码评审通知", channel.getChannelName());
                    channel.sendReviewNotification(task, result);
                    logger.debug("{} 渠道代码评审通知发送完成", channel.getChannelName());
                } catch (Exception e) {
                    logger.error("{} 渠道代码评审通知发送失败: repo={}, pr={}", 
                        channel.getChannelName(), task.repositoryFullName(), task.prNumber(), e);
                }
            }))
            .toList();

        // 等待所有通知发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result1, throwable) -> {
                if (throwable == null) {
                    logger.info("代码评审通知发送完成: repo={}, pr={}, 渠道数={}", 
                        task.repositoryFullName(), task.prNumber(), enabledChannels.size());
                } else {
                    logger.error("代码评审通知发送部分失败: repo={}, pr={}", 
                        task.repositoryFullName(), task.prNumber(), throwable);
                }
            });
    }

    @Override
    @Async("notificationExecutor")
    public void sendWikiCompletionNotification(String taskId, String repoName, int documentCount) {
        logger.info("开始发送Wiki文档生成完成通知: taskId={}, repo={}", taskId, repoName);

        List<NotificationChannel> enabledChannels = channelFactory.getEnabledChannels();
        
        if (enabledChannels.isEmpty()) {
            logger.debug("没有启用的通知渠道，跳过通知发送");
            return;
        }

        // 并行发送通知到所有启用的渠道
        List<CompletableFuture<Void>> futures = enabledChannels.stream()
            .map(channel -> CompletableFuture.runAsync(() -> {
                try {
                    logger.debug("通过 {} 渠道发送Wiki通知", channel.getChannelName());
                    channel.sendWikiNotification(taskId, repoName, documentCount);
                    logger.debug("{} 渠道Wiki通知发送完成", channel.getChannelName());
                } catch (Exception e) {
                    logger.error("{} 渠道Wiki通知发送失败: taskId={}, repo={}", 
                        channel.getChannelName(), taskId, repoName, e);
                }
            }))
            .toList();

        // 等待所有通知发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    logger.info("Wiki通知发送完成: taskId={}, repo={}, 渠道数={}", 
                        taskId, repoName, enabledChannels.size());
                } else {
                    logger.error("Wiki通知发送部分失败: taskId={}, repo={}", 
                        taskId, repoName, throwable);
                }
            });
    }
}
