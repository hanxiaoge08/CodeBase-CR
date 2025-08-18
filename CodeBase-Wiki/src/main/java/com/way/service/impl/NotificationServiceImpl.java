package com.way.service.impl;

import com.way.service.INotificationService;
import com.way.service.notification.NotificationChannel;
import com.way.service.notification.NotificationChannelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服务实现类
 * 支持多渠道异步通知发送
 */
@Service
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationChannelFactory channelFactory;

    public NotificationServiceImpl(NotificationChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    @Override
    @Async("notificationExecutor")
    public void sendWikiCompletionNotification(String taskId, String repoName, int documentCount) {
        log.info("开始发送Wiki文档生成完成通知: taskId={}, repo={}", taskId, repoName);

        List<NotificationChannel> enabledChannels = channelFactory.getEnabledChannels();
        
        if (enabledChannels.isEmpty()) {
            log.debug("没有启用的通知渠道，跳过通知发送");
            return;
        }

        // 并行发送通知到所有启用的渠道
        List<CompletableFuture<Void>> futures = enabledChannels.stream()
            .map(channel -> CompletableFuture.runAsync(() -> {
                try {
                    log.debug("通过 {} 渠道发送Wiki通知", channel.getChannelName());
                    channel.sendWikiNotification(taskId, repoName, documentCount);
                    log.debug("{} 渠道Wiki通知发送完成", channel.getChannelName());
                } catch (Exception e) {
                    log.error("{} 渠道Wiki通知发送失败: taskId={}, repo={}", 
                        channel.getChannelName(), taskId, repoName, e);
                }
            }))
            .toList();

        // 等待所有通知发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("Wiki通知发送完成: taskId={}, repo={}, 渠道数={}", 
                        taskId, repoName, enabledChannels.size());
                } else {
                    log.error("Wiki通知发送部分失败: taskId={}, repo={}", 
                        taskId, repoName, throwable);
                }
            });
    }

    @Override
    @Async("notificationExecutor")
    public void sendWikiFailureNotification(String taskId, String repoName, String errorMessage) {
        log.info("开始发送Wiki任务失败通知: taskId={}, repo={}", taskId, repoName);

        List<NotificationChannel> enabledChannels = channelFactory.getEnabledChannels();
        
        if (enabledChannels.isEmpty()) {
            log.debug("没有启用的通知渠道，跳过通知发送");
            return;
        }

        // 并行发送通知到所有启用的渠道
        List<CompletableFuture<Void>> futures = enabledChannels.stream()
            .map(channel -> CompletableFuture.runAsync(() -> {
                try {
                    log.debug("通过 {} 渠道发送Wiki失败通知", channel.getChannelName());
                    channel.sendWikiFailureNotification(taskId, repoName, errorMessage);
                    log.debug("{} 渠道Wiki失败通知发送完成", channel.getChannelName());
                } catch (Exception e) {
                    log.error("{} 渠道Wiki失败通知发送失败: taskId={}, repo={}", 
                        channel.getChannelName(), taskId, repoName, e);
                }
            }))
            .toList();

        // 等待所有通知发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("Wiki失败通知发送完成: taskId={}, repo={}, 渠道数={}", 
                        taskId, repoName, enabledChannels.size());
                } else {
                    log.error("Wiki失败通知发送部分失败: taskId={}, repo={}", 
                        taskId, repoName, throwable);
                }
            });
    }
}
