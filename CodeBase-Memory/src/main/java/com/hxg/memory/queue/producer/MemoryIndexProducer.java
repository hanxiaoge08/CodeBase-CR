package com.hxg.memory.queue.producer;

import com.hxg.memory.queue.model.MemoryIndexTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class MemoryIndexProducer {

    private static final Logger log = LoggerFactory.getLogger(MemoryIndexProducer.class);

    private final KafkaTemplate<String, MemoryIndexTask> kafkaTemplate;

    @Value("${project.memory.kafka.topics.mem-retry}")
    private String retryTopic;

    @Value("${project.memory.kafka.topics.mem-dlq}")
    private String dlqTopic;

    public MemoryIndexProducer(KafkaTemplate<String, MemoryIndexTask> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendToRetry(MemoryIndexTask task) {
        try {
            CompletableFuture<SendResult<String, MemoryIndexTask>> future =
                kafkaTemplate.send(retryTopic, buildKey(task), task);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("重试消息已发送: key={}, offset={}", buildKey(task), result.getRecordMetadata().offset());
                } else {
                    log.error("发送重试消息失败: key={}", buildKey(task), throwable);
                }
            });
        } catch (Exception e) {
            log.error("发送到重试队列失败: key={}", buildKey(task), e);
        }
    }

    public void sendToDlq(MemoryIndexTask task, Exception error) {
        try {
            CompletableFuture<SendResult<String, MemoryIndexTask>> future =
                kafkaTemplate.send(dlqTopic, buildKey(task), task);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.warn("死信消息已发送: key={}, error={}, offset={}", buildKey(task), error.getMessage(), result.getRecordMetadata().offset());
                } else {
                    log.error("发送死信消息失败: key={}, error={}", buildKey(task), error.getMessage(), throwable);
                }
            });
        } catch (Exception e) {
            log.error("发送到死信队列失败: key={}", buildKey(task), e);
        }
    }

    private String buildKey(MemoryIndexTask task) {
        if (task.getTaskId() != null) return task.getTaskId();
        if (task.getRepositoryId() != null && task.getDocumentName() != null) return task.getRepositoryId() + ":" + task.getDocumentName();
        if (task.getRepositoryId() != null && task.getFileName() != null) return task.getRepositoryId() + ":" + task.getFileName();
        return String.valueOf(System.currentTimeMillis());
    }
}


