package com.hxg.queue.producer;

import com.hxg.queue.model.MemoryIndexTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class MemoryIndexProducer {

    private final KafkaTemplate<String, MemoryIndexTask> kafkaTemplate;

    @Value("${project.wiki.kafka.topics.mem-index}")
    private String memIndexTopic;

    public MemoryIndexProducer(KafkaTemplate<String, MemoryIndexTask> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendDocumentTask(MemoryIndexTask task) {
        send(task);
    }

    public void sendCodeFileTask(MemoryIndexTask task) {
        send(task);
    }

    private void send(MemoryIndexTask task) {
        try {
            String key = buildKey(task);
            CompletableFuture<SendResult<String, MemoryIndexTask>> future = kafkaTemplate.send(memIndexTopic, key, task);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    log.info("Memory索引任务发送成功: key={}, offset={}", key, result.getRecordMetadata().offset());
                } else {
                    log.error("Memory索引任务发送失败: key={}", key, throwable);
                }
            });
        } catch (Exception e) {
            log.error("发送Memory索引任务到Kafka失败", e);
            throw new RuntimeException("Failed to send memory index task", e);
        }
    }

    private String buildKey(MemoryIndexTask task) {
        if (task.getTaskId() != null) return task.getTaskId();
        if (task.getRepositoryId() != null && task.getDocumentName() != null) return task.getRepositoryId() + ":" + task.getDocumentName();
        if (task.getRepositoryId() != null && task.getFileName() != null) return task.getRepositoryId() + ":" + task.getFileName();
        return String.valueOf(System.currentTimeMillis());
    }
}


