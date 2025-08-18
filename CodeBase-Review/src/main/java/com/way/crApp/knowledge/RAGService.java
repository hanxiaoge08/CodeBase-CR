package com.way.crApp.knowledge;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.*;
import com.way.crApp.service.ReviewESService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG服务
 * <p>
 * 职责：执行检索增强生成（RAG）中的"检索"步骤
 * 核心方法：retrieveContext(String codeSnippet, String repoFullName)
 * 流程：接收一小段代码 -> 在阿里云百炼知识库中执行向量搜索 -> 返回最相关的文本片段
 *
 */
@Service()
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    private static final String indexName = "编码规范知识库";
    
    @Autowired(required = false)
    private ReviewESService reviewMemoryService;

    @Value("classpath:/data/spring_ai_alibaba_quickstart.pdf")
    private Resource springAiResource;

    private final ChatClient chatClient;

    private final DashScopeApi dashscopeApi;

    public RAGService(ChatClient.Builder builder, DashScopeApi dashscopeApi) {
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
                DashScopeDocumentRetrieverOptions.builder().withIndexName(indexName).build());

        this.dashscopeApi = dashscopeApi;
        this.chatClient = builder
                .defaultAdvisors(new DocumentRetrievalAdvisor(retriever))
                .build();
    }

    public String retrieveContext(String codeSnippet, String repoFullName) {
        //修改  从wiki服务调用混合检索接口
        try {
            // 检查是否有记忆服务可用
            if (reviewMemoryService != null && reviewMemoryService.isWikiServiceAvailable()) {
                logger.info("使用记忆服务获取上下文: repoFullName={}", repoFullName);
                
                // 使用记忆服务搜索相关上下文
                return reviewMemoryService.searchContextForCodeReview(
                    repoFullName, 
                    codeSnippet, 
                    "代码分析", 
                    "分析代码片段并提供编码建议", 
                    List.of()
                );
            } else {
                logger.warn("记忆服务不可用，使用传统知识库查询");
                
                // 构建查询prompt，包含代码片段和仓库信息
                String queryPrompt = String.format(
                    "请根据以下代码片段查找相关的编码规范和最佳实践：\n\n" +
                    "仓库：%s\n" +
                    "代码片段：\n```\n%s\n```\n\n" +
                    "请提供与此代码相关的编码规范、代码质量要求和改进建议。",
                    repoFullName, codeSnippet
                );
                
                // 使用chatClient查询知识库，获取相关上下文
                String context = chatClient.prompt()
                    .user(queryPrompt)
                    .call()
                    .content();
                
                logger.debug("Retrieved context for code snippet from repository {}: {}", 
                    repoFullName, context.substring(0, Math.min(context.length(), 200)) + "...");
                
                return context;
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve context from knowledge base for repository {}: {}", 
                repoFullName, e.getMessage(), e);
            
            // 返回默认的编码规范提示
            return "请遵循以下基本编码规范：\n" +
                   "1. 代码应具有良好的可读性和可维护性\n" +
                   "2. 遵循命名约定和代码格式规范\n" +
                   "3. 添加适当的注释和文档\n" +
                   "4. 确保代码的性能和安全性\n" +
                   "5. 遵循项目特定的编码标准";
        }
    }
}