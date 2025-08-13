package com.hxg.queue.test;

import com.hxg.queue.model.DocumentGenerationTask;
import com.hxg.queue.producer.DocumentGenerationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author hxg
 * @description: Kafka消息流程测试
 * @date 2025/8/5
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "test.kafka.enabled", havingValue = "true")
public class KafkaMessageFlowTest implements CommandLineRunner {
    
    private final DocumentGenerationProducer producer;
    
    public KafkaMessageFlowTest(DocumentGenerationProducer producer) {
        this.producer = producer;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始Kafka消息流程测试...");
        
        // 创建测试任务
        DocumentGenerationTask testTask = new DocumentGenerationTask();
        testTask.setTaskId("test-task-" + System.currentTimeMillis());
        testTask.setCatalogueId("test-catalogue-001");
        testTask.setCatalogueName("测试文档生成");
        testTask.setPrompt("生成一个简单的测试文档");
        testTask.setLocalPath("C:\\Code\\CodeBase-CR");
        testTask.setFileTree("test-file-tree");
        testTask.setRetryCount(0);
        testTask.setCreateTime(LocalDateTime.now());
        testTask.setPriority("HIGH");
        
        // 发送测试消息
        try {
            producer.sendTask(testTask);
            log.info("测试消息发送成功: taskId={}", testTask.getTaskId());
        } catch (Exception e) {
            log.error("测试消息发送失败", e);
        }
        
        log.info("Kafka消息流程测试完成");
    }
}