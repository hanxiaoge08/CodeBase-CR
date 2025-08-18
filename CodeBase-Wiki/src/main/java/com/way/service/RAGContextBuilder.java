package com.way.service;

import com.way.model.dto.SearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG上下文构建器
 * 基于检索结果构建用于大模型的上下文信息
 * 
 * @author way
 */
@Service
public class RAGContextBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(RAGContextBuilder.class);
    
    private final EnhancedHybridSearchService enhancedHybridSearchService;
    
    @Autowired
    public RAGContextBuilder(EnhancedHybridSearchService enhancedHybridSearchService) {
        this.enhancedHybridSearchService = enhancedHybridSearchService;
    }

    /**
     * 构建RAG上下文
     * @param userQuery 用户查询
     * @param taskId 任务ID（可选）
     * @param maxResults 最大检索结果数
     * @param maxContextLength 最大上下文长度
     * @return 完整的RAG上下文
     */
    public RAGContext buildContext(String userQuery, String taskId, int maxResults, int maxContextLength) {
        logger.info("开始构建RAG上下文: query={}, taskId={}, maxResults={}", 
                userQuery, taskId, maxResults);

        try {
            // 1. 执行增强混合检索
            List<SearchResultDTO> searchResults = enhancedHybridSearchService.hybridSearch(userQuery, taskId, maxResults);
            
            if (searchResults.isEmpty()) {
                logger.warn("未找到相关检索结果，返回空上下文");
                return new RAGContext(userQuery, "", searchResults, false);
            }

            // 2. 构建上下文文本
            String contextText = buildContextText(searchResults, maxContextLength);
            
            // 3. 构建完整prompt
            String enhancedPrompt = buildEnhancedPrompt(userQuery, contextText);
            
            logger.info("RAG上下文构建完成: 使用{}个检索结果, 上下文长度={}", 
                    searchResults.size(), contextText.length());
            
            return new RAGContext(enhancedPrompt, contextText, searchResults, true);
            
        } catch (Exception e) {
            logger.error("构建RAG上下文失败: query={}, error={}", userQuery, e.getMessage(), e);
            return new RAGContext(userQuery, "", List.of(), false);
        }
    }

    /**
     * 构建上下文文本
     */
    private String buildContextText(List<SearchResultDTO> searchResults, int maxContextLength) {
        StringBuilder context = new StringBuilder();
        
        // 分类整理检索结果
        List<SearchResultDTO> codeResults = searchResults.stream()
                .filter(r -> "code".equals(r.getType()))
                .collect(Collectors.toList());
        
        List<SearchResultDTO> docResults = searchResults.stream()
                .filter(r -> "document".equals(r.getType()))
                .collect(Collectors.toList());

        // 添加代码块上下文
        if (!codeResults.isEmpty()) {
            context.append("## 相关代码片段\n\n");
            for (int i = 0; i < codeResults.size() && context.length() < maxContextLength; i++) {
                SearchResultDTO result = codeResults.get(i);
                String codeContext = formatCodeContext(result, i + 1);
                
                if (context.length() + codeContext.length() > maxContextLength) {
                    break;
                }
                context.append(codeContext).append("\n\n");
            }
        }

        // 添加文档上下文
        if (!docResults.isEmpty()) {
            context.append("## 相关文档\n\n");
            for (int i = 0; i < docResults.size() && context.length() < maxContextLength; i++) {
                SearchResultDTO result = docResults.get(i);
                String docContext = formatDocumentContext(result, i + 1);
                
                if (context.length() + docContext.length() > maxContextLength) {
                    break;
                }
                context.append(docContext).append("\n\n");
            }
        }

        return context.toString();
    }

    /**
     * 格式化代码上下文
     */
    private String formatCodeContext(SearchResultDTO result, int index) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append(String.format("### 代码片段 %d (相关度: %.2f)\n", index, result.getScore()));
        
        if (StringUtils.hasText(result.getApiName())) {
            formatted.append(String.format("**方法/函数:** %s\n", result.getApiName()));
        }
        
        if (StringUtils.hasText(result.getClassName())) {
            formatted.append(String.format("**类名:** %s\n", result.getClassName()));
        }
        
        if (StringUtils.hasText(result.getLanguage())) {
            formatted.append(String.format("**编程语言:** %s\n", result.getLanguage()));
        }
        
        if (StringUtils.hasText(result.getSummary())) {
            formatted.append(String.format("**说明:** %s\n", result.getSummary()));
        }
        
        formatted.append("\n**代码内容:**\n");
        formatted.append("```").append(result.getLanguage() != null ? result.getLanguage() : "").append("\n");
        formatted.append(truncateContent(result.getContent(), 800));
        formatted.append("\n```");
        
        return formatted.toString();
    }

    /**
     * 格式化文档上下文
     */
    private String formatDocumentContext(SearchResultDTO result, int index) {
        StringBuilder formatted = new StringBuilder();
        
        formatted.append(String.format("### 文档 %d (相关度: %.2f)\n", index, result.getScore()));
        
        if (StringUtils.hasText(result.getTitle())) {
            formatted.append(String.format("**标题:** %s\n", result.getTitle()));
        }
        
        formatted.append("\n**内容:**\n");
        formatted.append(truncateContent(result.getContent(), 1000));
        
        return formatted.toString();
    }

    /**
     * 构建增强的提示词
     */
    private String buildEnhancedPrompt(String userQuery, String contextText) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个专业的代码助手，擅长分析和解释代码。基于以下检索到的相关信息来回答用户的问题。\n\n");
        
        prompt.append("## 相关背景信息\n");
        prompt.append(contextText);
        prompt.append("\n\n");
        
        prompt.append("## 用户问题\n");
        prompt.append(userQuery);
        prompt.append("\n\n");
        
        prompt.append("## 回答要求\n");
        prompt.append("1. 基于上述背景信息提供准确的回答\n");
        prompt.append("2. 如果涉及代码，请结合具体代码片段进行说明\n");
        prompt.append("3. 如果背景信息不足以回答问题，请明确说明\n");
        prompt.append("4. 回答要简洁明了，重点突出\n");
        prompt.append("5. 使用中文回答\n\n");
        
        prompt.append("请基于以上信息回答用户的问题：");
        
        return prompt.toString();
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content != null ? content : "";
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * RAG上下文结果类
     */
    public static class RAGContext {
        private final String enhancedPrompt;
        private final String contextText;
        private final List<SearchResultDTO> searchResults;
        private final boolean hasContext;

        public RAGContext(String enhancedPrompt, String contextText, List<SearchResultDTO> searchResults, boolean hasContext) {
            this.enhancedPrompt = enhancedPrompt;
            this.contextText = contextText;
            this.searchResults = searchResults;
            this.hasContext = hasContext;
        }

        public String getEnhancedPrompt() {
            return enhancedPrompt;
        }

        public String getContextText() {
            return contextText;
        }

        public List<SearchResultDTO> getSearchResults() {
            return searchResults;
        }

        public boolean hasContext() {
            return hasContext;
        }

        public int getResultCount() {
            return searchResults.size();
        }
    }
}
