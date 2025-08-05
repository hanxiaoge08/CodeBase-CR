package com.hxg.queue.consumer;

import com.hxg.queue.model.DocumentGenerationTask;
import com.hxg.queue.producer.DocumentGenerationProducer;
import com.hxg.queue.service.DocumentProcessingService;
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
 * @author hxg
 * @description: 文档生成任务消费者
 * @date 2025/8/5
 */
@Component
@Slf4j
public class DocumentGenerationConsumer {
    
    private final DocumentProcessingService processingService;
    private final DocumentGenerationProducer producer;
    private final Semaphore concurrencyLimiter;
    
    @Value("${project.wiki.kafka.consumer.max-concurrency}")
    private int maxConcurrency;
    
    @Value("${project.wiki.kafka.consumer.process-interval}")
    private long processInterval;
    
    @Value("${project.wiki.kafka.consumer.max-retry}")
    private int maxRetry;
    
    public DocumentGenerationConsumer(DocumentProcessingService processingService,
                                    DocumentGenerationProducer producer) {
        this.processingService = processingService;
        this.producer = producer;
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
        try {
            // 获取并发控制许可
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
            
            // 处理成功，手动确认消息
            ack.acknowledge();
            log.info("任务处理完成: taskId={}, catalogueName={}", 
                    task.getTaskId(), task.getCatalogueName());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务处理被中断: taskId={}", task.getTaskId());
        } catch (Exception e) {
            log.error("任务处理失败: taskId={}, catalogueName={}, error={}", 
                    task.getTaskId(), task.getCatalogueName(), e.getMessage(), e);
            
            // 处理失败的任务
            handleFailedTask(task, e, isRetry);
            
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
}