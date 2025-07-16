package com.hxg.crApp.service.impl;

import com.hxg.crApp.adapter.github.GitHubAdapter;
import com.hxg.crApp.adapter.llm.LlmAdapter;
import com.hxg.crApp.dto.review.ReviewResultDTO;
import com.hxg.crApp.dto.review.ReviewTaskDTO;
//import com.hxg.crApp.knowledge.RAGService;
import com.hxg.crApp.knowledge.RAGService;
import com.hxg.crApp.service.port.ICodeReviewService;
import com.hxg.crApp.service.port.IKnowledgeBaseService;
import com.hxg.crApp.service.port.IResultPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 代码审查服务实现
 * 
 * 流程编排器：
 * 1. 调用 GitHubAdapter 获取 diff 内容
 * 2. 调用 RAGService，传入 diff 片段，检索相关的规范和代码上下文
 * 3. 调用 LlmAdapter，传入 diff 和检索到的上下文，获取AI生成的审查结果 ReviewResultDTO
 * 4. 调用 IResultPublishService 发布结果
 */
@Service
public class CodeReviewServiceImpl implements ICodeReviewService {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewServiceImpl.class);

    @Autowired
    private GitHubAdapter gitHubAdapter;

//    @Autowired
//    private RAGService ragService;

    @Autowired
    private LlmAdapter llmAdapter;

    @Autowired
    private IResultPublishService resultPublishService;
    @Autowired
    private RAGService ragService;

//    @Autowired
//    private IKnowledgeBaseService knowledgeBaseService;

    @Override
    public void performReview(ReviewTaskDTO task) {
        logger.info("开始执行代码审查: {}", task.repositoryFullName());
        
        try {
            // TODO:1. 确保知识库已构建
            //ensureKnowledgeBasesReady(task);
            
            // 2. 获取PR的diff内容
            String diffContent = gitHubAdapter.getPullRequestDiff(
                task.repositoryFullName(), 
                task.prNumber()
            );
            
            if (diffContent == null || diffContent.trim().isEmpty()) {
                logger.warn("未获取到diff内容，跳过审查: {}", task.repositoryFullName());
                return;
            }
            
            // TODO: 3. 使用RAG检索相关上下文
            String contextInfo = ragService.retrieveContext(diffContent, task.repositoryFullName());
            // 4. 调用LLM进行审查
            ReviewResultDTO result = llmAdapter.getReviewComments(
                diffContent, 
                contextInfo, 
                task.repositoryFullName()
            );
            
            // 5. 发布审查结果
            if (result != null && result.comments() != null && !result.comments().isEmpty()) {
                resultPublishService.publishReviewResult(task, result);
                logger.info("代码审查完成并发布: repo={}, pr={}, 评论数={}", 
                    task.repositoryFullName(), task.prNumber(), result.comments().size());
            } else {
                logger.info("未生成任何审查评论: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
            }
            
        } catch (Exception e) {
            logger.error("执行代码审查时发生错误: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber(), e);
        }
    }

//    /**
//     * 确保知识库已准备就绪
//     *
//     * @param task 审查任务
//     */
//    private void ensureKnowledgeBasesReady(ReviewTaskDTO task) {
//        // 确保规范知识库已构建
//        if (!knowledgeBaseService.isStyleGuideKnowledgeBaseBuilt()) {
//            logger.info("开始构建规范知识库...");
//            knowledgeBaseService.buildStyleGuideKnowledgeBase();
//        }
//
//        // 确保项目知识库已构建
//        if (!knowledgeBaseService.isProjectKnowledgeBaseBuilt(task.repositoryFullName())) {
//            logger.info("开始构建项目知识库: {}", task.repositoryFullName());
//            knowledgeBaseService.buildOrUpdateProjectKnowledgeBase(task.repositoryFullName());
//        }
//    }
} 