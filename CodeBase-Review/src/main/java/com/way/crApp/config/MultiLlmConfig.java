package com.way.crApp.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 多大模型支持配置类
 * 
 * 支持的模型类型：
 * 1. dashscope: 阿里云DashScope (默认)
 * 2. ollama: 本地Ollama模型
 * 3. openai: OpenAI模型
 * 
 * 通过配置 spring.ai.provider 来切换模型提供商
 */
@Configuration
public class MultiLlmConfig {

    private static final Logger logger = LoggerFactory.getLogger(MultiLlmConfig.class);

    @Value("${spring.ai.provider:dashscope}")
    private String llmProvider;

    /**
     * DashScope ChatModel 配置
     * 仅在 spring.ai.provider=dashscope 时生效（默认）
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.provider", havingValue = "dashscope", matchIfMissing = true)
    public ChatModel dashScopeChatModel(DashScopeChatModel dashScopeChatModel) {
        logger.info("初始化 DashScope ChatModel");
        return dashScopeChatModel;
    }

    /**
     * Ollama ChatModel 配置
     * 仅在 spring.ai.provider=ollama 时生效
     * 直接使用Spring AI的自动配置
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.provider", havingValue = "ollama")
    public ChatModel ollamaChatModel(OllamaChatModel ollamaChatModel) {
        logger.info("初始化 Ollama ChatModel，使用Spring AI自动配置");
        return ollamaChatModel;
    }

    /**
     * Ollama EmbeddingModel 配置
     * 仅在 spring.ai.provider=ollama 时生效
     * 直接使用Spring AI的自动配置
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel(OllamaEmbeddingModel ollamaEmbeddingModel) {
        logger.info("初始化 Ollama EmbeddingModel，使用Spring AI自动配置");
        return ollamaEmbeddingModel;
    }

    /**
     * OpenAI ChatModel 配置
     * 仅在 spring.ai.provider=openai 时生效
     * 直接使用Spring AI的自动配置
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.provider", havingValue = "openai")
    public ChatModel openAiChatModel(OpenAiChatModel openAiChatModel) {
        logger.info("初始化 OpenAI ChatModel，使用Spring AI自动配置");
        return openAiChatModel;
    }

    /**
     * OpenAI EmbeddingModel 配置
     * 仅在 spring.ai.provider=openai 时生效
     * 直接使用Spring AI的自动配置
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.provider", havingValue = "openai")
    public EmbeddingModel openAiEmbeddingModel(OpenAiEmbeddingModel openAiEmbeddingModel) {
        logger.info("初始化 OpenAI EmbeddingModel，使用Spring AI自动配置");
        return openAiEmbeddingModel;
    }

    /**
     * ChatClient 配置
     * 自动使用上面配置的 Primary ChatModel
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        logger.info("初始化 ChatClient，使用模型提供商: {}", llmProvider);
        return chatClientBuilder
                .defaultSystem("你是一个专业的Java代码审查专家，请根据提供的代码和上下文信息提供准确、具体的审查建议。")
                .build();
    }
}
