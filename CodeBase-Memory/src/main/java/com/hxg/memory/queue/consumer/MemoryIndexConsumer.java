package com.hxg.memory.queue.consumer;

import com.hxg.memory.queue.model.MemoryIndexTask;
import com.hxg.memory.queue.producer.MemoryIndexProducer;
import com.hxg.memory.service.DocumentMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class MemoryIndexConsumer {

    private static final Logger log = LoggerFactory.getLogger(MemoryIndexConsumer.class);

    private final DocumentMemoryService documentMemoryService;
    private final MemoryIndexProducer producer;
    private final Semaphore concurrencyLimiter;

    @Value("${project.memory.kafka.consumer.max-concurrency}")
    private int maxConcurrency;

    @Value("${project.memory.kafka.consumer.process-interval}")
    private long processIntervalMs;

    @Value("${project.memory.kafka.consumer.max-retry}")
    private int maxRetry;

    public MemoryIndexConsumer(DocumentMemoryService documentMemoryService, MemoryIndexProducer producer) {
        this.documentMemoryService = documentMemoryService;
        this.producer = producer;
        this.concurrencyLimiter = new Semaphore(2);
    }

    @KafkaListener(topics = "${project.memory.kafka.topics.mem-index}")
    public void consumeMain(@Payload MemoryIndexTask task,
                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset,
                            Acknowledgment ack) {
        log.info("接收到索引任务: type={}, repo={}, topic={}, partition={}, offset={}",
                task.getType(), task.getRepositoryId(), topic, partition, offset);

        processTask(task, ack, false);
    }

    @KafkaListener(topics = "${project.memory.kafka.topics.mem-retry}")
    public void consumeRetry(@Payload MemoryIndexTask task,
                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                             @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                             @Header(KafkaHeaders.OFFSET) long offset,
                             Acknowledgment ack) {
        log.info("接收到重试索引任务: type={}, repo={}, retryCount={}, topic={}, partition={}, offset={}",
                task.getType(), task.getRepositoryId(), task.getRetryCount(), topic, partition, offset);

        // 简单延迟，避免紧急重试
        try {
            Thread.sleep(Math.max(processIntervalMs, 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        processTask(task, ack, true);
    }

    private void processTask(MemoryIndexTask task, Acknowledgment ack, boolean isRetry) {
        boolean acquired = false;
        try {
            acquired = concurrencyLimiter.tryAcquire(10, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("无法获取并发许可，跳过任务: repo={}, type={}", task.getRepositoryId(), task.getType());
                return; // 不ack，等待重新投递
            }

            if (processIntervalMs > 0) {
                Thread.sleep(processIntervalMs);
            }

            validate(task);

            switch (task.getType()) {
                case "document" -> documentMemoryService
                        .addDocumentMemoryAsync(task.getRepositoryId(), task.getDocumentName(),
                                task.getDocumentContent(), task.getDocumentUrl(), task.getMetadata())
                        .join();
                case "code_file" -> documentMemoryService
                        .addCodeFileMemoryAsync(task.getRepositoryId(), task.getFileName(), task.getFilePath(),
                                task.getFileContent(), task.getFileType())
                        .join();
                default -> throw new IllegalArgumentException("未知的任务类型: " + task.getType());
            }

            ack.acknowledge();
            log.info("索引任务完成: type={}, repo={}", task.getType(), task.getRepositoryId());
        } catch (Exception e) {
            log.error("索引任务失败: type={}, repo={}, error={}", task.getType(), task.getRepositoryId(), e.getMessage(), e);
            handleFailure(task, e, isRetry);
            ack.acknowledge();
        } finally {
            if (acquired) {
                concurrencyLimiter.release();
            }
        }
    }

    private void validate(MemoryIndexTask task) {
        if (task == null) throw new IllegalArgumentException("任务为空");
        if (task.getType() == null) throw new IllegalArgumentException("任务类型为空");
        if (!Objects.equals(task.getType(), "document") && !Objects.equals(task.getType(), "code_file"))
            throw new IllegalArgumentException("非法任务类型: " + task.getType());
        if (task.getRepositoryId() == null || task.getRepositoryId().isEmpty())
            throw new IllegalArgumentException("repositoryId 不能为空");

        if (Objects.equals(task.getType(), "document")) {
            if (task.getDocumentName() == null || task.getDocumentContent() == null)
                throw new IllegalArgumentException("文档任务参数不完整");
        } else {
            if (task.getFileName() == null || task.getFileContent() == null)
                throw new IllegalArgumentException("代码文件任务参数不完整");
        }
    }

    private void handleFailure(MemoryIndexTask task, Exception error, boolean isRetry) {
        Integer retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
        retry++;
        task.setRetryCount(retry);

        if (retry > maxRetry) {
            log.warn("任务超过最大重试次数，进入DLQ: repo={}, type={}, retryCount={}", task.getRepositoryId(), task.getType(), retry);
            producer.sendToDlq(task, error);
        } else {
            log.info("任务失败，发送到重试队列: repo={}, type={}, retryCount={}", task.getRepositoryId(), task.getType(), retry);
            producer.sendToRetry(task);
        }
    }
}


