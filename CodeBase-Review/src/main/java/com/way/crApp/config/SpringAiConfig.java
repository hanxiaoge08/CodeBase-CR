package com.way.crApp.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * 
 * @deprecated 此类的功能已迁移到 MultiLlmConfig，支持多大模型切换
 * 保留此类主要用于配置 EmbeddingModel，其他功能请使用 MultiLlmConfig
 */
@Configuration
public class SpringAiConfig {

//    /**
//     * 配置向量存储
//     * 用于存储和检索知识库向量
//     *
//     * 注意：这里使用了自动配置的VectorStore，
//     * 具体实现（Chroma、Milvus等）通过application.yml配置
//     */
//    @Bean
//    public VectorStore vectorStore(VectorStore vectorStore) {
//        return vectorStore;
//    }

    /**
     * 配置嵌入模型
     * 用于将文本转换为向量
     * 
     * @deprecated 当使用 Ollama 时，此Bean将被 MultiLlmConfig 中的 ollamaEmbeddingModel 覆盖
     */
    @Bean
    public EmbeddingModel embeddingModel(EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
} 