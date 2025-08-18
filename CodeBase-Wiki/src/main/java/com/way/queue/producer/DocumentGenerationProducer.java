package com.way.queue.producer;

import com.way.queue.model.DocumentGenerationTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author way
 * @description: 文档生成任务生产者
 * @date 2025/8/5
 */
@Service
@Slf4j
public class DocumentGenerationProducer {
    
    private final KafkaTemplate<String, DocumentGenerationTask> kafkaTemplate;
    
    @Value("${project.wiki.kafka.topics.doc-generation}")
    private String docGenerationTopic;
    
    @Value("${project.wiki.kafka.topics.doc-retry}")
    private String docRetryTopic;
    
    @Value("${project.wiki.kafka.topics.doc-dlq}")
    private String docDlqTopic;
    
    public DocumentGenerationProducer(KafkaTemplate<String, DocumentGenerationTask> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        log.info("DocumentGenerationProducer initialized");
    }
    
    /**
     * 发送文档生成任务到主队列
     * @param task 文档生成任务
     */
    public void sendTask(DocumentGenerationTask task) {
        try {
            CompletableFuture<SendResult<String, DocumentGenerationTask>> future = 
                kafkaTemplate.send(docGenerationTopic, task.getTaskId(), task);
                
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("任务发送成功: taskId={}, catalogueName={}, offset={}", 
                        task.getTaskId(), task.getCatalogueName(), result.getRecordMetadata().offset());
                } else {
                    log.error("任务发送失败: taskId={}, catalogueName={}", 
                        task.getTaskId(), task.getCatalogueName(), throwable);
                }
            });
        } catch (Exception e) {
            log.error("发送任务到Kafka失败: taskId={}, catalogueName={}", 
                task.getTaskId(), task.getCatalogueName(), e);
            throw new RuntimeException("Failed to send task to Kafka", e);
        }
    }

    /**
     * 发送任务到重试队列
     * @param task 需要重试的任务
     */
    public void sendToRetryQueue(DocumentGenerationTask task) {
        try {
            CompletableFuture<SendResult<String, DocumentGenerationTask>> future = 
                kafkaTemplate.send(docRetryTopic, task.getTaskId(), task);
                
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("任务发送到重试队列成功: taskId={}, retryCount={}, offset={}", 
                        task.getTaskId(), task.getRetryCount(), result.getRecordMetadata().offset());
                } else {
                    log.error("任务发送到重试队列失败: taskId={}, retryCount={}", 
                        task.getTaskId(), task.getRetryCount(), throwable);
                }
            });
        } catch (Exception e) {
            log.error("发送任务到重试队列失败: taskId={}, retryCount={}", 
                task.getTaskId(), task.getRetryCount(), e);
        }
    }
    
    /**
     * 发送任务到死信队列
     * @param task 失败的任务
     * @param error 错误信息
     */
    public void sendToDeadLetterQueue(DocumentGenerationTask task, Exception error) {
        try {
            // 记录错误信息到任务中
            task.setPriority("FAILED");
            
            CompletableFuture<SendResult<String, DocumentGenerationTask>> future = 
                kafkaTemplate.send(docDlqTopic, task.getTaskId(), task);
                
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.warn("任务发送到死信队列: taskId={}, error={}, offset={}", 
                        task.getTaskId(), error.getMessage(), result.getRecordMetadata().offset());
                } else {
                    log.error("任务发送到死信队列失败: taskId={}, error={}", 
                        task.getTaskId(), error.getMessage(), throwable);
                }
            });
        } catch (Exception e) {
            log.error("发送任务到死信队列失败: taskId={}, originalError={}", 
                task.getTaskId(), error.getMessage(), e);
        }
    }
    
    /**
     * 获取主题名称
     */
    public String getDocGenerationTopic() {
        return docGenerationTopic;
    }
    
    public String getDocRetryTopic() {
        return docRetryTopic;
    }
    
    public String getDocDlqTopic() {
        return docDlqTopic;
    }
}