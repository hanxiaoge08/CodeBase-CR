package com.way.service;

import com.way.model.dto.CodeReviewContextRequest;
import com.way.model.dto.SearchResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码评审ES检索服务
 * 专门为代码评审场景提供上下文检索功能
 * 
 * @author way
 */
@Service
public class CodeReviewESService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeReviewESService.class);
    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 6000; // 代码评审上下文长度限制
    
    private final EnhancedHybridSearchService enhancedHybridSearchService;

    @Autowired
    public CodeReviewESService(EnhancedHybridSearchService enhancedHybridSearchService) {
        this.enhancedHybridSearchService = enhancedHybridSearchService;
    }

    /**
     * 为代码评审搜索相关上下文
     * @param request 代码评审上下文请求
     * @return 格式化的上下文字符串
     */
    public String searchContextForCodeReview(CodeReviewContextRequest request) {
        logger.info("开始为代码评审搜索上下文: repositoryId={}, prTitle={}, changedFiles={}",
                request.getRepositoryId(), request.getPrTitle(), request.getChangedFiles());

        try {
            // 构建搜索查询
            String searchQuery = buildSearchQuery(request);
            logger.info("构建的搜索查询: {}", searchQuery);
            
            // 确定任务ID，优先使用taskId，回退到repositoryId taskId和repositoryId一一对应的
            String taskId = StringUtils.hasText(request.getTaskId()) ? 
                    request.getTaskId() : request.getRepositoryId();
            
            // 获取最大结果数
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 10;
            
            // 执行混合检索
            List<SearchResultDTO> searchResults = enhancedHybridSearchService.hybridSearch(
                    searchQuery, taskId, maxResults);
            
            if (searchResults.isEmpty()) {
                logger.warn("未找到相关的代码评审上下文");
                return "";
            }
            
            // 格式化为代码评审上下文
            String formattedContext = formatCodeReviewContext(searchResults, request);
            
            logger.info("代码评审上下文检索完成: 找到{}个相关结果, 上下文长度={}字符", 
                    searchResults.size(), formattedContext.length());
            
            return formattedContext;
            
        } catch (Exception e) {
            logger.error("代码评审上下文检索失败: error={}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 重载方法，支持原有的参数形式
     */
    public String searchContextForCodeReview(String repositoryId, String diffContent, 
                                           String prTitle, String prDescription, 
                                           List<String> changedFiles) {
        CodeReviewContextRequest request = new CodeReviewContextRequest();
        request.setRepositoryId(repositoryId);
        request.setTaskId(repositoryId); // 兼容性处理
        request.setDiffContent(diffContent);
        request.setPrTitle(prTitle);
        request.setPrDescription(prDescription);
        request.setChangedFiles(changedFiles);
        request.setMaxResults(10);
        
        return searchContextForCodeReview(request);
    }

    /**
     * 构建搜索查询
     */
    private String buildSearchQuery(CodeReviewContextRequest request) {
        StringBuilder query = new StringBuilder();
        
        // 优先级1: PR标题
        if (StringUtils.hasText(request.getPrTitle())) {
            query.append(request.getPrTitle()).append(" ");
        }
        
        // 优先级2: 变更文件相关的关键词
        if (request.getChangedFiles() != null && !request.getChangedFiles().isEmpty()) {
            String fileKeywords = request.getChangedFiles().stream()
                    .map(this::extractFileKeywords)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(" "));
            if (StringUtils.hasText(fileKeywords)) {
                query.append(fileKeywords).append(" ");
            }
        }
        
        // 优先级3: PR描述 (截取关键部分)
        if (StringUtils.hasText(request.getPrDescription())) {
            String description = extractKeywordsFromDescription(request.getPrDescription());
            if (StringUtils.hasText(description)) {
                query.append(description).append(" ");
            }
        }
        
        // 优先级4: diff内容关键词 (提取重要信息)
        if (StringUtils.hasText(request.getDiffContent())) {
            String diffKeywords = extractKeywordsFromDiff(request.getDiffContent());
            if (StringUtils.hasText(diffKeywords)) {
                query.append(diffKeywords);
            }
        }
        
        String finalQuery = query.toString().trim();
        return StringUtils.hasText(finalQuery) ? finalQuery : "代码评审";
    }

    /**
     * 从文件路径提取关键词
     */
    private String extractFileKeywords(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return "";
        }
        
        // 提取文件名和关键路径部分
        String[] parts = filePath.split("/");
        StringBuilder keywords = new StringBuilder();
        
        for (String part : parts) {
            if (StringUtils.hasText(part) && !part.equals("src") && !part.equals("main") 
                && !part.equals("java") && !part.equals("resources")) {
                // 提取类名或重要目录名
                String keyword = part.replaceAll("\\.(java|kt|scala|groovy)$", "");
                keywords.append(keyword).append(" ");
            }
        }
        
        return keywords.toString().trim();
    }

    /**
     * 从PR描述中提取关键词
     */
    private String extractKeywordsFromDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return "";
        }
        
        // 限制长度，提取前200个字符的关键信息
        String truncated = description.length() > 200 ? description.substring(0, 200) : description;
        
        // 移除一些常见的非关键词
        return truncated.replaceAll("(?i)(fix|add|update|remove|refactor|improve)\\s+", "")
                       .replaceAll("[\\r\\n]+", " ")
                       .trim();
    }

    /**
     * 从diff内容中提取关键词
     */
    private String extractKeywordsFromDiff(String diffContent) {
        if (!StringUtils.hasText(diffContent)) {
            return "";
        }
        
        StringBuilder keywords = new StringBuilder();
        String[] lines = diffContent.split("\\r?\\n");
        int addedLines = 0;
        
        for (String line : lines) {
            if (addedLines >= 10) break; // 限制处理的行数
            
            if (line.startsWith("+ ") || line.startsWith("- ")) {
                // 提取新增或删除行的关键词
                String code = line.substring(2).trim();
                if (StringUtils.hasText(code) && !code.startsWith("//") && !code.startsWith("*")) {
                    // 提取方法名、类名等关键词
                    String extracted = extractCodeKeywords(code);
                    if (StringUtils.hasText(extracted)) {
                        keywords.append(extracted).append(" ");
                        addedLines++;
                    }
                }
            }
        }
        
        return keywords.toString().trim();
    }

    /**
     * 从代码行中提取关键词
     */
    private String extractCodeKeywords(String codeLine) {
        // 简单的关键词提取，可以根据需要进一步优化
        return codeLine.replaceAll("[{}();,]", " ")
                      .replaceAll("\\s+", " ")
                      .trim();
    }

    /**
     * 格式化代码评审上下文
     */
    private String formatCodeReviewContext(List<SearchResultDTO> searchResults, 
                                         CodeReviewContextRequest request) {
        StringBuilder context = new StringBuilder();
        
        context.append("## 代码评审相关上下文\n\n");
        
        // 添加PR基本信息
        if (StringUtils.hasText(request.getPrTitle())) {
            context.append("**PR标题**: ").append(request.getPrTitle()).append("\n");
        }
        
        if (request.getChangedFiles() != null && !request.getChangedFiles().isEmpty()) {
            context.append("**变更文件**: ").append(String.join(", ", request.getChangedFiles())).append("\n\n");
        }
        
        // 分类显示检索结果
        List<SearchResultDTO> codeResults = searchResults.stream()
                .filter(r -> "code".equals(r.getType()))
                .collect(Collectors.toList());
        
        List<SearchResultDTO> docResults = searchResults.stream()
                .filter(r -> "document".equals(r.getType()))
                .collect(Collectors.toList());

        // 添加相关代码片段
        if (!codeResults.isEmpty()) {
            context.append("### 相关代码片段\n\n");
            int codeIndex = 1;
            for (SearchResultDTO result : codeResults) {
                if (context.length() > DEFAULT_MAX_CONTEXT_LENGTH) break;
                
                context.append(String.format("#### 代码片段 %d (相关度: %.2f)\n", codeIndex++, result.getScore()));
                
                if (StringUtils.hasText(result.getApiName())) {
                    context.append("**方法**: ").append(result.getApiName()).append("\n");
                }
                
                if (StringUtils.hasText(result.getClassName())) {
                    context.append("**类**: ").append(result.getClassName()).append("\n");
                }
                
                if (StringUtils.hasText(result.getSummary())) {
                    context.append("**说明**: ").append(result.getSummary()).append("\n");
                }
                
                context.append("\n```").append(result.getLanguage() != null ? result.getLanguage() : "").append("\n");
                context.append(truncateContent(result.getContent(), 500));
                context.append("\n```\n\n");
            }
        }

        // 添加相关文档
        if (!docResults.isEmpty()) {
            context.append("### 相关文档\n\n");
            int docIndex = 1;
            for (SearchResultDTO result : docResults) {
                if (context.length() > DEFAULT_MAX_CONTEXT_LENGTH) break;
                
                context.append(String.format("#### 文档 %d (相关度: %.2f)\n", docIndex++, result.getScore()));
                
                if (StringUtils.hasText(result.getTitle())) {
                    context.append("**标题**: ").append(result.getTitle()).append("\n\n");
                }
                
                context.append(truncateContent(result.getContent(), 300)).append("\n\n");
            }
        }

        return context.toString();
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
     * 检查服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            return enhancedHybridSearchService.isServiceAvailable();
        } catch (Exception e) {
            logger.debug("代码评审ES服务不可用: {}", e.getMessage());
            return false;
        }
    }
}