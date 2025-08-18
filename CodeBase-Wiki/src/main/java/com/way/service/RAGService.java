package com.way.service;

import com.way.model.dto.ChatResponseDTO;
import com.way.model.dto.RAGSearchResponseDTO;
import com.way.model.dto.SearchOnlyResponseDTO;
import com.way.model.dto.SearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        logger.info("处理RAG对话: query={}, taskId={}, useRAG={}, topK={}", query, taskId, useRAG, topK);
        
        try {
            ChatResponseDTO response = new ChatResponseDTO();
            response.setQuery(query);
            response.setUseRAG(useRAG);
            
            if (!useRAG) {
                // 不使用RAG，直接调用原始chat
                String aiResponse = chatClientBuilder
                        .defaultOptions(ToolCallingChatOptions.builder()
                                .toolCallbacks(allTools)
                                .build())
                        .build()
                        .prompt(query)
                        .call()
                        .content();
                
                response.setResponse(aiResponse);
                response.setHasContext(false);
                response.setContextCount(0);
                
            } else {
                // 使用RAG增强的对话
                int k = topK != null ? topK : DEFAULT_TOP_K;
                RAGContextBuilder.RAGContext ragContext = ragContextBuilder.buildContext(query, taskId, k, DEFAULT_CONTEXT_LENGTH);
                
                if (!ragContext.hasContext()) {
                    // 如果没有找到相关上下文，回退到普通对话
                    String aiResponse = chatClientBuilder
                            .defaultOptions(ToolCallingChatOptions.builder()
                                    .toolCallbacks(allTools)
                                    .build())
                            .build()
                            .prompt("没有找到相关的代码或文档信息，请基于常识回答：" + query)
                            .call()
                            .content();
                    
                    response.setResponse(aiResponse);
                    response.setHasContext(false);
                    response.setContextCount(0);
                    
                } else {
                    // 使用RAG增强的prompt调用大模型
                    String aiResponse = chatClientBuilder
                            .build()
                            .prompt(ragContext.getEnhancedPrompt())
                            .call()
                            .content();
                    
                    // 添加检索结果信息
                    if (ragContext.getResultCount() > 0) {
                        aiResponse += String.format("\n\n---\n💡 基于 %d 个相关代码片段和文档生成此回答", ragContext.getResultCount());
                    }
                    
                    response.setResponse(aiResponse);
                    response.setHasContext(true);
                    response.setContextCount(ragContext.getResultCount());
                }
            }
            
            logger.info("RAG对话处理完成: hasContext={}, contextCount={}", response.getHasContext(), response.getContextCount());
            return response;
            
        } catch (Exception e) {
            logger.error("RAG对话处理失败: query={}, error={}", query, e.getMessage(), e);
            throw new RuntimeException("对话处理失败：" + e.getMessage(), e);
        }
    }

    /**
     * 处理RAG检索并生成回答
     * @param query 用户查询
     * @param taskId 任务ID（可选）
     * @param topK TopK数量
     * @return RAG检索响应
     */
    public RAGSearchResponseDTO processSearchWithRAG(String query, String taskId, Integer topK) {
        logger.info("处理RAG检索: query={}, taskId={}, topK={}", query, taskId, topK);
        
        try {
            int k = topK != null ? topK : DEFAULT_TOP_K;
            
            // 执行增强混合检索
            List<SearchResultDTO> searchResults = enhancedHybridSearchService.hybridSearch(query, taskId, k);
            
            // 构建RAG上下文并生成回答
            RAGContextBuilder.RAGContext ragContext = ragContextBuilder.buildContext(query, taskId, k, DEFAULT_CONTEXT_LENGTH);
            
            String aiResponse = "";
            if (ragContext.hasContext()) {
                aiResponse = chatClientBuilder
                        .build()
                        .prompt(ragContext.getEnhancedPrompt())
                        .call()
                        .content();
            } else {
                aiResponse = "抱歉，没有找到相关的代码或文档信息来回答您的问题。";
            }
            
            RAGSearchResponseDTO response = new RAGSearchResponseDTO();
            response.setQuery(query);
            response.setSearchResults(searchResults);
            response.setAiResponse(aiResponse);
            response.setResultCount(searchResults.size());
            response.setHasContext(ragContext.hasContext());
            
            logger.info("RAG检索处理完成: 检索结果{}个, hasContext={}", searchResults.size(), ragContext.hasContext());
            return response;
            
        } catch (Exception e) {
            logger.error("RAG检索处理失败: query={}, error={}", query, e.getMessage(), e);
            throw new RuntimeException("检索过程中发生错误：" + e.getMessage(), e);
        }
    }

    /**
     * 处理纯检索（不生成AI回答）
     * @param query 用户查询
     * @param taskId 任务ID（可选）
     * @param topK TopK数量
     * @return 纯检索响应
     */
    public SearchOnlyResponseDTO processSearchOnly(String query, String taskId, Integer topK) {
        logger.info("处理纯检索: query={}, taskId={}, topK={}", query, taskId, topK);
        
        try {
            int k = topK != null ? topK : DEFAULT_TOP_K;
            
            List<SearchResultDTO> searchResults = enhancedHybridSearchService.hybridSearch(query, taskId, k);
            
            SearchOnlyResponseDTO response = new SearchOnlyResponseDTO();
            response.setQuery(query);
            response.setSearchResults(searchResults);
            response.setResultCount(searchResults.size());
            
            logger.info("纯检索处理完成: 返回{}个结果", searchResults.size());
            return response;
            
        } catch (Exception e) {
            logger.error("纯检索处理失败: query={}, error={}", query, e.getMessage(), e);
            throw new RuntimeException("检索过程中发生错误：" + e.getMessage(), e);
        }
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
}
