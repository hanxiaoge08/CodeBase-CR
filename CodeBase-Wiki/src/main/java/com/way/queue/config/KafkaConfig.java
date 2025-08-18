package com.way.queue.config;

import com.way.queue.model.DocumentGenerationTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author way
 * @description: Kafka配置类
 * @date 2025/8/5
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${project.wiki.kafka.consumer.max-concurrency}")
    private int maxConcurrency;
    
    /**
     * 生产者工厂配置
     */
    @Bean
    public ProducerFactory<String, DocumentGenerationTask> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        
        log.info("Kafka Producer configured with bootstrap servers: {}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka模板
     */
    @Bean
    public KafkaTemplate<String, DocumentGenerationTask> kafkaTemplate() {
        KafkaTemplate<String, DocumentGenerationTask> template = new KafkaTemplate<>(producerFactory());
        log.info("KafkaTemplate created for DocumentGenerationTask");
        return template;
    }


    /**
     * 消费者工厂配置
     */
    @Bean
    public ConsumerFactory<String, DocumentGenerationTask> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        
        // 配置JSON反序列化器的类型信息
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.way.queue.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DocumentGenerationTask.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        log.info("Kafka Consumer configured with group-id: {}, bootstrap servers: {}", groupId, bootstrapServers);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), 
                new JsonDeserializer<>(DocumentGenerationTask.class));
    }

    /**
     * Kafka监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentGenerationTask> 
           kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DocumentGenerationTask> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(maxConcurrency); // 设置并发消费者数量
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        log.info("Kafka Listener Container Factory configured with concurrency: {}", maxConcurrency);
        return factory;
    }
}