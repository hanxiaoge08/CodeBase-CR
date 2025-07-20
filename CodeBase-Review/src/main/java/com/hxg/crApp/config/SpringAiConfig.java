package com.hxg.crApp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * 
 * 集中管理 ChatClient, EmbeddingClient, 和 VectorStore 的创建
 * 这使得切换LLM提供商（如从OpenAI到Ollama）或向量数据库（从Chroma到Milvus）只需修改此文件
 */
@Configuration
public class SpringAiConfig {

    /**
     * 配置 ChatClient Bean
     * 用于与大语言模型进行对话
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个专业的Java代码审查专家，请根据提供的代码和上下文信息提供准确、具体的审查建议。")
                .build();
    }

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
     */
    @Bean
    public EmbeddingModel embeddingModel(EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
} 