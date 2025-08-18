package com.way.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 向量嵌入服务
 * 使用阿里云DashScope的text-embedding-v4模型生成向量嵌入
 * 
 * @author way
 */
@Service
public class VectorEmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorEmbeddingService.class);
    
    private final EmbeddingModel embeddingModel;

    @Autowired
    public VectorEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        logger.info("VectorEmbeddingService 初始化完成，使用模型: text-embedding-v4");
    }

    /**
     * 同步生成文本的向量嵌入
     * @param text 待嵌入的文本
     * @return 向量数组，失败时返回null
     */
    public float[] generateEmbedding(String text) {
        if (!StringUtils.hasText(text)) {
            logger.debug("文本内容为空，跳过向量生成");
            return null;
        }

        try {
            // 限制文本长度，避免超过模型限制
            String processedText = truncateText(text, 8000);
            
            logger.debug("开始生成向量嵌入，文本长度: {}", processedText.length());
            
            EmbeddingRequest request = new EmbeddingRequest(List.of(processedText), null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            if (response != null && !response.getResults().isEmpty()) {
                float[] embedding = response.getResults().get(0).getOutput();
                
                logger.debug("向量嵌入生成成功，维度: {}", embedding.length);
                return embedding;
            } else {
                logger.warn("向量嵌入生成失败：响应为空");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("生成向量嵌入时发生异常: textLength={}, error={}", 
                    text.length(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 异步生成文本的向量嵌入
     * @param text 待嵌入的文本
     * @return CompletableFuture<float[]>
     */
    public CompletableFuture<float[]> generateEmbeddingAsync(String text) {
        return CompletableFuture.supplyAsync(() -> generateEmbedding(text));
    }

    /**
     * 为代码块生成向量嵌入
     * 结合类名、方法名、文档摘要和代码内容
     * @param className 类名
     * @param methodName 方法名
     * @param docSummary 文档摘要
     * @param content 代码内容
     * @return 向量数组
     */
    public float[] generateCodeEmbedding(String className, String methodName, 
                                       String docSummary, String content) {
        StringBuilder combinedText = new StringBuilder();
        
        if (StringUtils.hasText(className)) {
            combinedText.append("类名: ").append(className).append("\n");
        }
        
        if (StringUtils.hasText(methodName)) {
            combinedText.append("方法名: ").append(methodName).append("\n");
        }
        
        if (StringUtils.hasText(docSummary)) {
            combinedText.append("文档摘要: ").append(docSummary).append("\n");
        }
        
        if (StringUtils.hasText(content)) {
            combinedText.append("代码内容: ").append(content);
        }
        
        return generateEmbedding(combinedText.toString());
    }

    /**
     * 异步为代码块生成向量嵌入
     */
    public CompletableFuture<float[]> generateCodeEmbeddingAsync(String className, String methodName, 
                                                               String docSummary, String content) {
        return CompletableFuture.supplyAsync(() -> 
                generateCodeEmbedding(className, methodName, docSummary, content));
    }

    /**
     * 为文档生成向量嵌入
     * 结合文档标题和内容
     * @param title 文档标题
     * @param content 文档内容
     * @return 向量数组
     */
    public float[] generateDocumentEmbedding(String title, String content) {
        StringBuilder combinedText = new StringBuilder();
        
        if (StringUtils.hasText(title)) {
            combinedText.append("标题: ").append(title).append("\n\n");
        }
        
        if (StringUtils.hasText(content)) {
            combinedText.append(content);
        }
        
        return generateEmbedding(combinedText.toString());
    }

    /**
     * 异步为文档生成向量嵌入
     */
    public CompletableFuture<float[]> generateDocumentEmbeddingAsync(String title, String content) {
        return CompletableFuture.supplyAsync(() -> generateDocumentEmbedding(title, content));
    }

    /**
     * 检查嵌入服务是否可用
     * @return 是否可用
     */
    public boolean isServiceAvailable() {
        try {
            // 使用简单文本测试服务可用性
            float[] testVector = generateEmbedding("测试文本");
            return testVector != null && testVector.length > 0;
        } catch (Exception e) {
            logger.debug("向量嵌入服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 截断文本以避免超过模型限制
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        
        String truncated = text.substring(0, maxLength);
        logger.debug("文本已截断: 原长度={}, 截断后长度={}", text.length(), truncated.length());
        return truncated;
    }


}
