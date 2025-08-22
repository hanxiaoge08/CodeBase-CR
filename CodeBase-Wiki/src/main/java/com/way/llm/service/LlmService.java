package com.way.llm.service;

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * @author way
 * @description: LLM 服务类
 * @date 2025/7/20 00:42
 */
@Service
@Slf4j
public class LlmService {
    private final ChatClient chatClient;
    private final ChatClient chatClientWithoutMemory;
    private final MessageWindowChatMemory chatMemory;
    private final ToolCallback[] allTools;

    public LlmService(ChatClient.Builder chatClientBuilder, SQLiteChatMemoryRepository sqliteChatMemoryRepository, ToolCallback[] allTools) {
        int maxHistoryMessages = 20;
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(sqliteChatMemoryRepository)
                .maxMessages(maxHistoryMessages)
                .build();
        // 带记忆的ChatClient（用于聊天）
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        // 不带记忆的ChatClient（用于后台任务）
        this.chatClientWithoutMemory = chatClientBuilder
                .clone()  // 克隆builder避免状态干扰
                .build();
        this.allTools = allTools;
    }

    public String callWithTools(String query){
        return chatClient
                .prompt(query)
                .advisors(
                    a->a.param(CONVERSATION_ID, UUID.randomUUID().toString())
                )
                .options(ToolCallingChatOptions.builder().toolCallbacks(allTools).build())
                .call()
                .content();
    }

    /**
     * 调用LLM服务但不记录对话历史（用于文档生成等任务）
     * @param query 查询内容
     * @return LLM响应结果
     */
    public String callWithToolsWithoutMemory(String query) {
        return chatClientWithoutMemory
                .prompt(query)
                .options(ToolCallingChatOptions.builder().toolCallbacks(allTools).build())
                .call()
                .content();
    }

    public String callWithoutTools(String query) {
        return chatClient
                .prompt(query)
                .advisors(
                        a -> a.param(CONVERSATION_ID, cn.hutool.core.lang.UUID.randomUUID().toString())
                )
                .call()
                .content();
    }
    public Flux<String> chatWithTools(String query, String conversationId) {
        return chatClient
                .prompt(query)
                .advisors(a -> a.param(CONVERSATION_ID, StringUtils.isEmpty(conversationId) ? MDC.get("traceId") : conversationId))
                .options(
                        ToolCallingChatOptions.builder().toolCallbacks(allTools).build())
                .stream()
                .content();
    }

    /**
     * 与模型和工具交互的对话方法，支持流式响应
     * 
     * @param query 用户输入的查询内容
     * @param model 使用的模型名称
     * @param conversationId 会话标识，为空时使用traceId替代
     * @return Flux<String> 流式响应数据
     * 注：该方法通过chatClient实现以下核心功能：
     * 1. 设置会话上下文参数CONVERSATION_ID
     * 2. 构建包含模型配置和工具回调的选项
     * 3. 返回流式响应内容
     */
    public Flux<String> chatWithModelAndTools(String query, String model, String conversationId) {
        return chatClient
                .prompt(query)
                // 配置会话参数：优先使用传入的conversationId，否则使用traceId
                .advisors(a -> a.param(CONVERSATION_ID,
                        StringUtils.isEmpty(conversationId) ? MDC.get("traceId") : conversationId))
                // 构建工具调用选项：指定模型并注册所有工具回调
                .options(
                        ToolCallingChatOptions.builder()
                                .model(model)
                                .toolCallbacks(allTools)
                                .build()
                )
                .stream()
                .content();
    }

}
