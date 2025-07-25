package com.alibaba.example.chatmemory.config;

import com.alibaba.example.chatmemory.service.CodeReviewMemoryService;
import com.alibaba.example.chatmemory.service.FunctionCallMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Spring AI Function配置
 * 注册RAG搜索函数供AI调用
 * 
 * @author AI Assistant
 */
@Configuration
@ConditionalOnBean(CodeReviewMemoryService.class)
public class SpringAIFunctionConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringAIFunctionConfig.class);
    
    @Autowired
    private CodeReviewMemoryService codeReviewMemoryService;
    
    @Autowired
    private FunctionCallMonitorService monitorService;
    
    /**
     * 注册代码评审上下文搜索函数
     */
    @Bean
    @Description("为代码评审搜索相关的项目上下文信息，包括相关文档和代码文件")
    public Function<CodeReviewSearchRequest, String> searchCodeReviewContext() {
        return request -> {
            
            String callId = monitorService.recordFunctionCallStart(
                "searchCodeReviewContext", 
                request.repositoryId(), 
                String.format("PR: %s, Files: %d", request.prTitle(), 
                    request.changedFiles() != null ? request.changedFiles().size() : 0)
            );
            
            try {
                logger.info("AI调用代码评审搜索函数: repositoryId={}, prTitle={}", 
                    request.repositoryId(), request.prTitle());
                
                String result = codeReviewMemoryService.searchContextForCodeReview(
                    request.repositoryId(),
                    request.diffContent(),
                    request.prTitle(),
                    request.prDescription(),
                    request.changedFiles()
                );
                
                monitorService.recordFunctionCallEnd(callId, true, result.length());
                return result;
                
            } catch (Exception e) {
                logger.error("代码评审搜索函数执行失败", e);
                String errorResult = "无法获取相关上下文信息，将基于现有信息进行评审。";
                monitorService.recordFunctionCallEnd(callId, false, errorResult.length());
                return errorResult;
            }
        };
    }
    
    /**
     * 注册特定内容类型搜索函数
     */
    @Bean
    @Description("按内容类型搜索项目中的特定信息，如文档或代码文件")
    public Function<ContentTypeSearchRequest, List<CodeReviewMemoryService.MemorySearchResult>> searchByContentType() {
        return request -> {
            try {
                logger.info("AI调用内容类型搜索函数: repositoryId={}, contentType={}, query={}", 
                    request.repositoryId(), request.contentType(), request.query());
                
                return codeReviewMemoryService.searchByContentType(
                    request.repositoryId(),
                    request.query(),
                    request.contentType(),
                    request.limit()
                );
                
            } catch (Exception e) {
                logger.error("内容类型搜索函数执行失败", e);
                return Collections.emptyList();
            }
        };
    }
    
    /**
     * 注册文档搜索函数
     */
    @Bean
    @Description("搜索项目中的相关文档内容")
    public Function<DocumentSearchRequest, List<CodeReviewMemoryService.MemorySearchResult>> searchRelatedDocuments() {
        return request -> {
            try {
                logger.info("AI调用文档搜索函数: repositoryId={}, query={}", 
                    request.repositoryId(), request.query());
                
                return codeReviewMemoryService.searchRelatedDocuments(
                    request.repositoryId(),
                    request.query(),
                    request.limit()
                );
                
            } catch (Exception e) {
                logger.error("文档搜索函数执行失败", e);
                return Collections.emptyList();
            }
        };
    }
    
    /**
     * 注册代码文件搜索函数
     */
    @Bean
    @Description("搜索项目中的相关代码文件")
    public Function<CodeFileSearchRequest, List<CodeReviewMemoryService.MemorySearchResult>> searchRelatedCodeFiles() {
        return request -> {
            try {
                logger.info("AI调用代码文件搜索函数: repositoryId={}, query={}", 
                    request.repositoryId(), request.query());
                
                return codeReviewMemoryService.searchRelatedCodeFiles(
                    request.repositoryId(),
                    request.query(),
                    request.limit()
                );
                
            } catch (Exception e) {
                logger.error("代码文件搜索函数执行失败", e);
                return Collections.emptyList();
            }
        };
    }
    
    /**
     * 代码评审搜索请求记录
     */
    public record CodeReviewSearchRequest(
        @Description("仓库ID，用于标识搜索范围") String repositoryId,
        @Description("PR的diff内容") String diffContent,
        @Description("PR标题") String prTitle,
        @Description("PR描述") String prDescription,
        @Description("变更的文件列表") List<String> changedFiles
    ) {}
    
    /**
     * 内容类型搜索请求记录
     */
    public record ContentTypeSearchRequest(
        @Description("仓库ID") String repositoryId,
        @Description("搜索查询内容") String query,
        @Description("内容类型：document(文档) 或 code_file(代码文件)") String contentType,
        @Description("返回结果数量限制，默认为5") int limit
    ) {
        public ContentTypeSearchRequest {
            if (limit <= 0) limit = 5;
        }
    }
    
    /**
     * 文档搜索请求记录
     */
    public record DocumentSearchRequest(
        @Description("仓库ID") String repositoryId,
        @Description("搜索查询内容") String query,
        @Description("返回结果数量限制，默认为5") int limit
    ) {
        public DocumentSearchRequest {
            if (limit <= 0) limit = 5;
        }
    }
    
    /**
     * 代码文件搜索请求记录
     */
    public record CodeFileSearchRequest(
        @Description("仓库ID") String repositoryId,
        @Description("搜索查询内容") String query,
        @Description("返回结果数量限制，默认为3") int limit
    ) {
        public CodeFileSearchRequest {
            if (limit <= 0) limit = 3;
        }
    }
} 