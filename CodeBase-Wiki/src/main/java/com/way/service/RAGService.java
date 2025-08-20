package com.way.service;

import com.way.model.dto.ChatResponseDTO;
import com.way.model.dto.RAGSearchResponseDTO;
import com.way.model.dto.SearchOnlyResponseDTO;
import com.way.model.dto.SearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;



/**
 * RAG (Retrieval-Augmented Generation) 服务
 * 负责处理基于检索增强生成的对话和搜索逻辑
 * 
 * @author way
 */
@Service
public class RAGService {
    
    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);
    private static final int DEFAULT_TOP_K = 10;
    private static final int DEFAULT_CONTEXT_LENGTH = 8000;
    
    private final ChatClient.Builder chatClientBuilder;
    private final ToolCallback[] allTools;
    private final EnhancedHybridSearchService enhancedHybridSearchService;
    private final RAGContextBuilder ragContextBuilder;
    

    
    @Autowired
    private ChatMemoryService chatMemoryService;

    @Autowired
    public RAGService(ChatClient.Builder chatClientBuilder,
                     ToolCallback[] allTools,
                     EnhancedHybridSearchService enhancedHybridSearchService,
                     RAGContextBuilder ragContextBuilder) {
        this.chatClientBuilder = chatClientBuilder;
        this.allTools = allTools;
        this.enhancedHybridSearchService = enhancedHybridSearchService;
        this.ragContextBuilder = ragContextBuilder;
    }

    /**
     * 处理基于RAG的对话
     * @param query 用户查询
     * @param taskId 任务ID（可选）
     * @param useRAG 是否使用RAG
     * @param topK TopK数量
     * @return 对话响应
     */
    public ChatResponseDTO processChat(String query, String taskId, boolean useRAG, Integer topK) {
        return processChat(query, taskId, useRAG, topK, null);
    }
    
    /**
     * 处理基于RAG的对话（完整参数版本）
     * @param query 用户查询
     * @param taskId 任务ID（可选）
     * @param useRAG 是否使用RAG
     * @param topK TopK数量
     * @param userId 用户ID（可选，用于记忆隔离）
     * @return 对话响应
     */
    public ChatResponseDTO processChat(String query, String taskId, boolean useRAG, Integer topK, String userId) {
        return executeWithTimedErrorHandling("RAG对话", query, () -> {
            ChatResponseDTO response = new ChatResponseDTO();
            response.setQuery(query);
            response.setUseRAG(useRAG);
            
            // 获取有效用户ID和聊天记忆
            String effectiveUserId = StringUtils.hasText(userId) ? userId : getUserIdFromRequest();
            ChatMemoryContext memoryContext = buildChatMemoryContext(taskId, effectiveUserId);
            
            // 生成AI回答
            String aiResponse;
            if (!useRAG) {
                aiResponse = processNonRAGChat(query, memoryContext);
                response.setResponse(aiResponse);
                response.setHasContext(false);
                response.setContextCount(0);
            } else {
                ChatResult chatResult = processRAGChat(query, taskId, topK, memoryContext);
                response.setResponse(chatResult.response);
                response.setHasContext(chatResult.hasContext);
                response.setContextCount(chatResult.contextCount);
                aiResponse = chatResult.response;
            }
            
            // 保存对话记录
            saveChatConversation(taskId, effectiveUserId, query, aiResponse);
            return response;
        });
    }

    /**
     * 处理RAG检索并生成回答
     */
    public RAGSearchResponseDTO processSearchWithRAG(String query, String taskId, Integer topK) {
        return executeWithErrorHandling("RAG检索", () -> {
            int k = topK != null ? topK : DEFAULT_TOP_K;
            List<SearchResultDTO> searchResults = enhancedHybridSearchService.hybridSearch(query, taskId, k);
            RAGContextBuilder.RAGContext ragContext = ragContextBuilder.buildContext(query, taskId, k, DEFAULT_CONTEXT_LENGTH);
            
            String aiResponse = ragContext.hasContext() 
                ? callChatClient(ragContext.getEnhancedPrompt(), false)
                : "抱歉，没有找到相关的代码或文档信息来回答您的问题。";
            
            RAGSearchResponseDTO response = new RAGSearchResponseDTO();
            response.setQuery(query);
            response.setSearchResults(searchResults);
            response.setAiResponse(aiResponse);
            response.setResultCount(searchResults.size());
            response.setHasContext(ragContext.hasContext());
            
            logger.info("RAG检索处理完成: 检索结果{}个, hasContext={}", searchResults.size(), ragContext.hasContext());
            return response;
        });
    }

    /**
     * 处理纯检索（不生成AI回答）
     */
    public SearchOnlyResponseDTO processSearchOnly(String query, String taskId, Integer topK) {
        return executeWithErrorHandling("纯检索", () -> {
            int k = topK != null ? topK : DEFAULT_TOP_K;
            List<SearchResultDTO> searchResults = enhancedHybridSearchService.hybridSearch(query, taskId, k);
            
            SearchOnlyResponseDTO response = new SearchOnlyResponseDTO();
            response.setQuery(query);
            response.setSearchResults(searchResults);
            response.setResultCount(searchResults.size());
            
            logger.info("纯检索处理完成: 返回{}个结果", searchResults.size());
            return response;
        });
    }

    /**
     * 获取TopK上下文结果
     * 从检索结果中选择最相关的内容作为上下文
     * @param searchResults 检索结果
     * @param topK TopK数量
     * @return 筛选后的上下文结果
     */
    public List<SearchResultDTO> getTopKContextResults(List<SearchResultDTO> searchResults, int topK) {
        if (searchResults == null || searchResults.isEmpty()) {
            return searchResults;
        }
        
        // 按分数排序并取TopK
        return searchResults.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查RAG服务是否可用
     * @return 服务可用性状态
     */
    public boolean isServiceAvailable() {
        try {
            return enhancedHybridSearchService.isServiceAvailable();
        } catch (Exception e) {
            logger.debug("RAG服务不可用: {}", e.getMessage());
            return false;
        }
    }
    

    

    

    
    /**
     * 获取固定的用户ID
     * 简化实现：所有用户共享同一个固定ID
     * @return 固定的用户ID
     */
    private String getUserIdFromRequest() {
        // 写死一个固定的用户ID，所有请求共享
        return "default_user";
    }
    
    // ==================== 通用辅助方法 ====================
    

    
    /**
     * 带错误处理的通用执行模板
     */
    private <T> T executeWithErrorHandling(String operation, SearchOperation<T> operation_func) {
        logger.info("处理{}: query={}", operation, "...");
        try {
            return operation_func.execute();
        } catch (Exception e) {
            logger.error("{}处理失败: error={}", operation, e.getMessage(), e);
            throw new RuntimeException(operation + "过程中发生错误：" + e.getMessage(), e);
        }
    }
    
    /**
     * 带时间记录和错误处理的执行模板
     */
    private <T> T executeWithTimedErrorHandling(String operation, String query, SearchOperation<T> operation_func) {
        long startTime = System.currentTimeMillis();
        logger.info("处理{}: query={}", operation, query);
        
        try {
            T result = operation_func.execute();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (result instanceof ChatResponseDTO) {
                ChatResponseDTO response = (ChatResponseDTO) result;
                logger.info("{}处理完成: hasContext={}, contextCount={}, responseTime={}ms", 
                           operation, response.getHasContext(), response.getContextCount(), responseTime);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("{}处理失败: query={}, error={}", operation, query, e.getMessage(), e);
            throw new RuntimeException("对话处理失败：" + e.getMessage(), e);
        }
    }
    

    
    /**
     * 构建聊天记忆上下文
     */
    private ChatMemoryContext buildChatMemoryContext(String taskId, String effectiveUserId) {
        if (!StringUtils.hasText(taskId)) {
            return new ChatMemoryContext(null, null);
        }
        
        ChatMemory taskChatMemory = chatMemoryService.getChatMemory(taskId, effectiveUserId);
        String conversationId = generateConversationId(taskId, effectiveUserId);
        List<Message> historyMessages = taskChatMemory.get(conversationId);
        
        logger.info("获取用户{}在任务{}的聊天记忆，历史消息数量: {}", effectiveUserId, taskId, historyMessages.size());
        return new ChatMemoryContext(taskChatMemory, conversationId);
    }
    
    /**
     * 构建历史对话上下文字符串
     */
    private String buildHistoryContext(ChatMemoryContext memoryContext, String basePrompt) {
        if (memoryContext.chatMemory == null || memoryContext.conversationId == null) {
            return basePrompt;
        }
        
        List<Message> historyMessages = memoryContext.chatMemory.get(memoryContext.conversationId);
        if (historyMessages.isEmpty()) {
            return basePrompt;
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("以下是历史对话记录：\n");
        for (Message msg : historyMessages) {
            String role = "user".equals(msg.getMessageType().getValue()) ? "用户" : "助手";
            String content = msg.toString();
            contextBuilder.append(role).append(": ").append(content).append("\n");
        }
        contextBuilder.append("\n").append(basePrompt);
        return contextBuilder.toString();
    }
    
    /**
     * 调用ChatClient获取AI回复
     */
    private String callChatClient(String prompt, boolean withTools) {
        ChatClient.Builder builder = chatClientBuilder;
        if (withTools) {
            builder = builder.defaultOptions(ToolCallingChatOptions.builder()
                    .toolCallbacks(allTools)
                    .build());
        }
        return builder.build().prompt(prompt).call().content();
    }
    
    /**
     * 处理非RAG聊天
     */
    private String processNonRAGChat(String query, ChatMemoryContext memoryContext) {
        String contextPrompt = buildHistoryContext(memoryContext, "当前问题: " + query);
        return callChatClient(contextPrompt, true);
    }
    
    /**
     * 处理RAG聊天
     */
    private ChatResult processRAGChat(String query, String taskId, Integer topK, ChatMemoryContext memoryContext) {
        int k = topK != null ? topK : DEFAULT_TOP_K;
        RAGContextBuilder.RAGContext ragContext = ragContextBuilder.buildContext(query, taskId, k, DEFAULT_CONTEXT_LENGTH);
        
        if (!ragContext.hasContext()) {
            // 没有RAG上下文，回退到记忆对话
            String basePrompt = "没有找到相关的代码或文档信息，请基于对话历史和常识回答：" + query;
            String contextPrompt = buildHistoryContext(memoryContext, basePrompt);
            String response = callChatClient(contextPrompt, true);
            return new ChatResult(response, false, 0);
        } else {
            // 有RAG上下文，整合记忆
            String enhancedPrompt = buildHistoryContext(memoryContext, ragContext.getEnhancedPrompt());
            String response = callChatClient(enhancedPrompt, false);
            
            // 添加检索结果信息
            if (ragContext.getResultCount() > 0) {
                response += String.format("\n\n---\n💡 基于 %d 个相关代码片段和文档以及对话历史生成此回答", ragContext.getResultCount());
            }
            
            return new ChatResult(response, true, ragContext.getResultCount());
        }
    }
    
    /**
     * 保存对话记录
     */
    private void saveChatConversation(String taskId, String effectiveUserId, String query, String aiResponse) {
        if (StringUtils.hasText(taskId)) {
            try {
                chatMemoryService.addCleanConversation(taskId, effectiveUserId, query, aiResponse);
            } catch (Exception e) {
                logger.error("保存纯净对话记录失败: taskId={}, userId={}, error={}", taskId, effectiveUserId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 生成会话 ID
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 会话 ID
     */
    private String generateConversationId(String taskId, String userId) {
        if (!StringUtils.hasText(taskId)) {
            return "general_" + (StringUtils.hasText(userId) ? userId : "anonymous");
        }
        
        if (!StringUtils.hasText(userId)) {
            userId = "default_user";
        }
        
        return "task_" + taskId + "_user_" + userId;
    }
    
    // ==================== 函数式接口 ====================
    
    @FunctionalInterface
    private interface SearchOperation<T> {
        T execute() throws Exception;
    }
    
    // ==================== 内部数据类 ====================
    
    /**
     * 聊天记忆上下文
     */
    private static class ChatMemoryContext {
        final ChatMemory chatMemory;
        final String conversationId;
        
        ChatMemoryContext(ChatMemory chatMemory, String conversationId) {
            this.chatMemory = chatMemory;
            this.conversationId = conversationId;
        }
    }
    
    /**
     * 聊天结果
     */
    private static class ChatResult {
        final String response;
        final boolean hasContext;
        final int contextCount;
        
        ChatResult(String response, boolean hasContext, int contextCount) {
            this.response = response;
            this.hasContext = hasContext;
            this.contextCount = contextCount;
        }
    }
}
