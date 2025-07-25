package com.hxg.crApp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 增强的ChatClient配置
 * 集成Spring AI Function调用功能
 * 
 * @author AI Assistant
 */
@Configuration
public class EnhancedChatConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedChatConfig.class);
    
    /**
     * 创建支持Function调用的ChatClient
     */
    @Bean
    @Primary
    public ChatClient enhancedChatClient(ChatClient.Builder chatClientBuilder) {
        
        logger.info("配置增强的ChatClient，支持Function调用");
        
        return chatClientBuilder
            .defaultSystem("""
                你是一个专业的代码评审助手，具备以下能力：
                
                1. **上下文搜索能力**：你可以调用搜索函数获取项目的相关文档和代码信息
                2. **代码分析能力**：基于项目上下文提供准确的代码评审建议
                3. **最佳实践推荐**：结合项目实际情况推荐编码最佳实践
                
                当进行代码评审时，你应该：
                - 首先分析PR的变更内容
                - 根据需要调用搜索函数获取相关上下文
                - 基于项目上下文提供具体、可操作的评审建议
                - 关注代码质量、安全性、性能和可维护性
                
                可用的搜索函数：
                - searchCodeReviewContext: 为代码评审搜索完整的上下文信息
                - searchByContentType: 按内容类型搜索特定信息
                - searchRelatedDocuments: 搜索相关文档
                - searchRelatedCodeFiles: 搜索相关代码文件
                
                请保持专业、友好的语调，并提供具体的改进建议。
                """)
            .build();
    }
} 