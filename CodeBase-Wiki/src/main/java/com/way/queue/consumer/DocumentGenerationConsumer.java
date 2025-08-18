package com.way.queue.consumer;

import com.way.queue.model.DocumentGenerationTask;
import com.way.queue.producer.DocumentGenerationProducer;
import com.way.queue.service.DocumentProcessingService;
import com.way.queue.service.DocumentProcessingService.TaskDeletedException;
import com.way.service.INotificationService;
import com.way.service.ITaskProgressService;
import com.way.service.ITaskService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author way
 * @description: 文档生成任务消费者
 * @date 2025/8/5
 */
@Component
@Slf4j
public class DocumentGenerationConsumer {

    private final DocumentProcessingService processingService;
    private final DocumentGenerationProducer producer;
    private final Semaphore concurrencyLimiter;
    private final ITaskProgressService taskProgressService;
    private final INotificationService notificationService;
    private final ITaskService taskService;

    @Value("${project.wiki.kafka.consumer.max-concurrency}")
    private int maxConcurrency;

    @Value("${project.wiki.kafka.consumer.process-interval}")
    private long processInterval;

    @Value("${project.wiki.kafka.consumer.max-retry}")
    private int maxRetry;

    public DocumentGenerationConsumer(DocumentProcessingService processingService,
                                      DocumentGenerationProducer producer,
                                      ITaskProgressService taskProgressService,
                                      INotificationService notificationService,
                                      ITaskService taskService) {
        this.processingService = processingService;
        this.producer = producer;
        this.taskProgressService = taskProgressService;
        this.notificationService = notificationService;
        this.taskService = taskService;
        this.concurrencyLimiter = new Semaphore(2); // 默认2个并发
    }

    @PostConstruct
    public void initConcurrencyLimiter() {
        concurrencyLimiter.drainPermits();
        concurrencyLimiter.release(maxConcurrency);
        log.info("DocumentGenerationConsumer initialized with maxConcurrency={}, processInterval={}ms, maxRetry={}",
                maxConcurrency, processInterval, maxRetry);
    }

    /**
     * 主队列消费者
     */
    @KafkaListener(topics = "${project.wiki.kafka.topics.doc-generation}")
    public void consumeMainQueue(@Payload DocumentGenerationTask task,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment ack) {

        log.info("接收到文档生成任务: taskId={}, catalogueName={}, topic={}, partition={}, offset={}",
                task.getTaskId(), task.getCatalogueName(), topic, partition, offset);

        processTask(task, ack, false);
    }

    /**
     * 重试队列消费者
     */
    @KafkaListener(topics = "${project.wiki.kafka.topics.doc-retry}")
    public void consumeRetryQueue(@Payload DocumentGenerationTask task,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment ack) {

        log.info("接收到重试任务: taskId={}, catalogueName={}, retryCount={}, topic={}, partition={}, offset={}",
                task.getTaskId(), task.getCatalogueName(), task.getRetryCount(), topic, partition, offset);

        // 重试队列的任务需要延迟处理
        try {
            Thread.sleep(30000); // 30秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("重试任务延迟被中断: taskId={}", task.getTaskId());
        }

        processTask(task, ack, true);
    }

    /**
     * 处理任务的通用方法
     */
    private void processTask(DocumentGenerationTask task, Acknowledgment ack, boolean isRetry) {
        boolean acquired = false;
        long startTime = System.currentTimeMillis();

        try {
            // 1. 幂等性检查
            ITaskProgressService.IdempotentResult idempotentResult = 
                taskProgressService.checkIdempotent(task.getCatalogueId());
                
            switch (idempotentResult) {
                case COMPLETED:
                    log.info("消息已处理完成，跳过处理: catalogueId={}", task.getCatalogueId());
                    ack.acknowledge();
                    return;
                case PROCESSING:
                    log.warn("消息正在处理中，稍后重试: catalogueId={}", task.getCatalogueId());
                    throw new RuntimeException("消息正在处理中，需要重试");
                case FIRST_TIME:
                    log.debug("消息第一次处理: catalogueId={}", task.getCatalogueId());
                    break;
            }

            // 2. 获取并发控制许可
            acquired = concurrencyLimiter.tryAcquire(10, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("无法获取并发许可，跳过任务: taskId={}", task.getTaskId());
                return; // 不ack，消息会重新投递
            }

            // 控制处理间隔
            if (processInterval > 0) {
                Thread.sleep(processInterval);
            }

            log.info("开始处理文档生成任务: taskId={}, catalogueName={}, retryCount={}",
                    task.getTaskId(), task.getCatalogueName(), task.getRetryCount());

            // 调用处理服务
            processingService.processTask(task);

            // 处理成功，标记幂等性完成
            taskProgressService.markMessageCompleted(task.getCatalogueId());
            
            // 手动确认消息
            ack.acknowledge();

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("任务处理完成: taskId={}, catalogueName={}, 耗时={}ms",
                    task.getTaskId(), task.getCatalogueName(), processingTime);

            // 增加完成任务数并检查是否需要发送通知
            long completedCount = taskProgressService.incrementConsumedCount(task.getTaskId());
            log.debug("任务完成计数更新: taskId={}, completedCount={}", task.getTaskId(), completedCount);

            // 检查是否所有任务都已完成
            if (taskProgressService.isTaskCompleted(task.getTaskId())) {
                log.info("检测到任务全部完成，准备发送通知: taskId={}", task.getTaskId());
                
                // 获取任务进度信息
                long totalCount = taskProgressService.getTaskTotal(task.getTaskId());
                
                // 获取仓库名称
                String repoName = getRepoNameFromTask(task);
                
                // 发送Wiki任务完成通知
                try {
                    notificationService.sendWikiCompletionNotification(
                        task.getTaskId(), 
                        repoName, 
                        (int) totalCount
                    );
                    log.info("Wiki完成通知已发送: taskId={}, repo={}, documentCount={}", 
                        task.getTaskId(), repoName, totalCount);
                } catch (Exception notificationException) {
                    log.error("发送Wiki完成通知失败: taskId={}, repo={}", 
                        task.getTaskId(), repoName, notificationException);
                }

                // 清理Redis中的任务进度数据
                try {
                    taskProgressService.clearTaskProgress(task.getTaskId());
                    log.info("任务进度数据已清理: taskId={}", task.getTaskId());
                } catch (Exception cleanupException) {
                    log.error("清理任务进度数据失败: taskId={}", task.getTaskId(), cleanupException);
                }
            }


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务处理被中断: taskId={}", task.getTaskId());
        } catch (TaskDeletedException e) {
            // 任务已删除，直接确认消息，不进行重试
            log.info("任务已删除，跳过处理并确认消息: taskId={}, reason={}", task.getTaskId(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("任务处理失败: taskId={}, catalogueName={}, error={}",
                    task.getTaskId(), task.getCatalogueName(), e.getMessage(), e);

            // 处理失败的任务
            handleFailedTask(task, e, isRetry);

            // 发送失败通知（如果超过最大重试次数）
            if (task.exceedsMaxRetries(maxRetry)) {
                try {
                    String repoName = getRepoNameFromTask(task);
                    notificationService.sendWikiFailureNotification(
                        task.getTaskId(), 
                        repoName, 
                        e.getMessage()
                    );
                    log.info("Wiki失败通知已发送: taskId={}, repo={}", task.getTaskId(), repoName);
                } catch (Exception notificationException) {
                    log.error("发送Wiki失败通知失败: taskId={}", task.getTaskId(), notificationException);
                }
            }

            // 确认消息，避免无限重试
            ack.acknowledge();
        } finally {
            if (acquired) {
                concurrencyLimiter.release();
            }
        }
    }

    /**
     * 处理失败的任务
     */
    private void handleFailedTask(DocumentGenerationTask task, Exception error, boolean isRetry) {
        // 增加重试次数
        task.incrementRetryCount();

        // 检查是否超过最大重试次数
        if (task.exceedsMaxRetries(maxRetry)) {
            log.warn("任务超过最大重试次数，发送到死信队列: taskId={}, retryCount={}, maxRetry={}",
                    task.getTaskId(), task.getRetryCount(), maxRetry);
            producer.sendToDeadLetterQueue(task, error);
        } else {
            log.info("任务处理失败，发送到重试队列: taskId={}, retryCount={}/{}",
                    task.getTaskId(), task.getRetryCount(), maxRetry);
            producer.sendToRetryQueue(task);
        }
    }

    /**
     * 获取当前并发许可数
     */
    public int getAvailablePermits() {
        return concurrencyLimiter.availablePermits();
    }

    /**
     * 获取最大并发数配置
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
    
    /**
     * 从任务中获取仓库名称
     *
     * @param task 文档生成任务
     * @return 仓库名称
     */
    private String getRepoNameFromTask(DocumentGenerationTask task) {
        try {
            // 先尝试从projectName获取
            if (task.getProjectName() != null && !task.getProjectName().isEmpty()) {
                return task.getProjectName();
            }
            
            // 从数据库获取任务信息
            com.way.model.entity.Task taskEntity = taskService.getTaskByTaskId(task.getTaskId());
            if (taskEntity != null && taskEntity.getProjectName() != null) {
                return taskEntity.getProjectName();
            }
            
            // 如果都获取不到，使用taskId作为默认值
            log.warn("无法获取仓库名称，使用taskId作为默认值: taskId={}", task.getTaskId());
            return task.getTaskId();
            
        } catch (Exception e) {
            log.error("获取仓库名称失败: taskId={}", task.getTaskId(), e);
            return task.getTaskId();
        }
    }
}