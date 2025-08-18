package com.way.crApp.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.way.crApp.adapter.github.GitHubAdapter;
import com.way.crApp.adapter.llm.LlmAdapter;
import com.way.crApp.dto.review.ReviewCommentDTO;
import com.way.crApp.dto.review.ReviewResultDTO;
import com.way.crApp.dto.review.ReviewTaskDTO;
import com.way.crApp.knowledge.RAGService;
import com.way.crApp.service.port.ICodeReviewService;
import com.way.crApp.service.port.INotificationService;
import com.way.crApp.service.port.IResultPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码审查服务实现
 * 
 * 整合了新的多智能体Graph工作流：
 * 1. 接收ReviewTaskDTO
 * 2. 获取PR的diff内容  
 * 3. 调用多智能体Graph工作流进行审查
 * 4. 处理和发布审查结果
 */
@Service
public class CodeReviewServiceImpl implements ICodeReviewService {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewServiceImpl.class);
    
    @Value("${review.use-graph-workflow:true}")
    private boolean useGraphWorkflow;

    @Autowired
    private GitHubAdapter gitHubAdapter;

    @Autowired(required = false)
    private CompiledGraph compiledCodeReviewGraph;

//    @Autowired
//    private RAGService ragService;

    @Autowired
    private LlmAdapter llmAdapter;

    @Autowired
    private IResultPublishService resultPublishService;
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private INotificationService notificationService;

    @Override
    public void performReview(ReviewTaskDTO task) {
        logger.info("开始执行代码审查: {}, 使用Graph工作流: {}", 
            task.repositoryFullName(), useGraphWorkflow);
        
        try {
            // 检查是否使用新的Graph工作流
            if (useGraphWorkflow && compiledCodeReviewGraph != null) {
                performGraphBasedReview(task);
            } else {
                // 降级到传统审查流程
                performTraditionalReview(task);
            }
        } catch (Exception e) {
            logger.error("执行代码审查时发生错误: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber(), e);
        }
    }
    
    /**
     * 使用多智能体Graph工作流执行审查
     */
    private void performGraphBasedReview(ReviewTaskDTO task) {
        logger.info("使用多智能体Graph工作流进行审查: repo={}, pr={}", 
            task.repositoryFullName(), task.prNumber());
        
        try {
            // 1. 获取PR的diff内容
            String diffContent = gitHubAdapter.getPullRequestDiff(
                task.repositoryFullName(), 
                task.prNumber()
            );
            
            if (diffContent == null || diffContent.trim().isEmpty()) {
                logger.warn("未获取到diff内容，跳过审查: {}", task.repositoryFullName());
                return;
            }
            
            // 2. 构建Graph工作流的初始状态
            Map<String, Object> initialState = buildGraphInitialState(task, diffContent);
            
            // 3. 执行Graph工作流
            long startTime = System.currentTimeMillis();
            java.util.Optional<OverAllState> result = compiledCodeReviewGraph.invoke(initialState);
            
            if (result.isEmpty()) {
                logger.error("Graph工作流执行返回空结果: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
                return;
            }
            
            OverAllState finalState = result.get();
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.info("Graph工作流执行完成: repo={}, pr={}, 耗时={}ms", 
                task.repositoryFullName(), task.prNumber(), executionTime);
            
            // 4. 处理工作流结果
            processGraphWorkflowResult(finalState, task);
            
        } catch (Exception e) {
            logger.error("Graph工作流执行失败，降级到传统流程: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber(), e);
            performTraditionalReview(task);
        }
    }
    
    /**
     * 传统审查流程（作为降级方案）
     */
    private void performTraditionalReview(ReviewTaskDTO task) {
        logger.info("使用传统流程进行代码审查: {}", task.repositoryFullName());
        
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
            
            ReviewResultDTO finalNotificationResult;
            
            // 5. 发布审查结果
            if (result != null && result.comments() != null && !result.comments().isEmpty()) {
                resultPublishService.publishReviewResult(task, result);
                logger.info("代码审查完成并发布: repo={}, pr={}, 评论数={}", 
                    task.repositoryFullName(), task.prNumber(), result.comments().size());
                finalNotificationResult = result;
            } else {
                // 即使没有发现问题，也发布审查通过报告
                logger.info("传统流程审查未发现问题，发布审查通过报告: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
                
                // 构建简单的审查通过结果
                String summary = "✅ 代码审查通过\n\n经过AI审查，未发现需要修改的问题。";
                ReviewResultDTO approvalResult = new ReviewResultDTO(
                    new java.util.ArrayList<>(),
                    summary,
                    ReviewResultDTO.OverallRating.EXCELLENT.getValue()
                );
                resultPublishService.publishReviewResult(task, approvalResult);
                finalNotificationResult = approvalResult;
            }
            
            // 发送代码评审完成通知
            try {
                logger.info("开始发送传统流程代码评审完成通知: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
                
                notificationService.sendReviewCompletionNotification(task, finalNotificationResult);
                
                logger.debug("传统流程代码评审完成通知已提交发送: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
            } catch (Exception notificationException) {
                // 通知发送失败不应该影响主流程
                logger.error("发送传统流程代码评审完成通知时发生错误: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber(), notificationException);
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
    
    /**
     * 构建Graph工作流的初始状态
     */
    private Map<String, Object> buildGraphInitialState(ReviewTaskDTO task, String diffContent) {
        Map<String, Object> initialState = new HashMap<>();
        
        // 基础任务信息
        initialState.put("review_task", task);
        initialState.put("diff_content", diffContent);
        initialState.put("repo_name", task.repositoryFullName());
        initialState.put("pr_number", task.prNumber());
        initialState.put("pr_title", task.prTitle());
        initialState.put("pr_author", task.prAuthor());
        initialState.put("head_sha", task.headSha());
        initialState.put("base_sha", task.baseSha());
        
        // 获取变更文件列表
        String[] changedFiles = extractChangedFiles(diffContent);
        initialState.put("changed_files", changedFiles);
        
        // 审查配置
        Map<String, Object> reviewConfig = new HashMap<>();
        reviewConfig.put("enable_style_check", true);
        reviewConfig.put("enable_logic_check", true);
        reviewConfig.put("enable_security_check", true);
        reviewConfig.put("memory_service_enabled", true);
        initialState.put("review_config", reviewConfig);
        
        // 工作流控制
        initialState.put("workflow_started", System.currentTimeMillis());
        initialState.put("workflow_status", "starting");
        
        logger.debug("构建Graph初始状态完成，包含 {} 个键", initialState.size());
        
        return initialState;
    }
    
    /**
     * 从diff内容中提取变更文件列表
     */
    private String[] extractChangedFiles(String diffContent) {
        List<String> files = new java.util.ArrayList<>();
        
        for (String line : diffContent.lines().toList()) {
            if (line.startsWith("diff --git")) {
                // 提取文件路径，格式如：diff --git a/path/to/file b/path/to/file
                String[] parts = line.split(" ");
                if (parts.length >= 4) {
                    String filePath = parts[2].substring(2); // 移除 "a/" 前缀
                    files.add(filePath);
                }
            }
        }
        
        logger.debug("从diff中提取到 {} 个变更文件", files.size());
        return files.toArray(new String[0]);
    }
    
    /**
     * 处理Graph工作流的执行结果
     */
    private void processGraphWorkflowResult(OverAllState finalState, ReviewTaskDTO task) {
        try {
            // 获取审查结果
            ReviewResultDTO finalResult = (ReviewResultDTO) finalState.value("final_review_result").orElse(null);
            
            // 如果没有直接的ReviewResultDTO，尝试从各个Agent的结果构建
            if (finalResult == null) {
                finalResult = buildReviewResultFromAgents(finalState);
            }
            
            Integer totalIssues = (Integer) finalState.value("total_issues").orElse(0);
            
            logger.info("Graph工作流审查完成: repo={}, pr={}, 总问题数={}", 
                task.repositoryFullName(), task.prNumber(), totalIssues);
            
            ReviewResultDTO finalNotificationResult;
            
            // 发布审查结果
            if (finalResult != null && finalResult.comments() != null && !finalResult.comments().isEmpty()) {
                resultPublishService.publishReviewResult(task, finalResult);
                logger.info("已发布审查结果: repo={}, pr={}, 评论数={}", 
                    task.repositoryFullName(), task.prNumber(), finalResult.comments().size());
                finalNotificationResult = finalResult;
            } else {
                // 即使没有发现问题，也应该发布审查通过的报告
                logger.info("审查未发现问题，准备发布审查通过报告: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
                
                // 构建审查通过的结果
                ReviewResultDTO approvalResult = buildApprovalResult(finalState);
                resultPublishService.publishReviewResult(task, approvalResult);
                logger.info("已发布审查通过报告: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
                finalNotificationResult = approvalResult;
            }

            // 发送代码评审完成通知
            try {
                logger.info("开始发送代码评审完成通知: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
                
                notificationService.sendReviewCompletionNotification(task, finalNotificationResult);
                
                logger.debug("代码评审完成通知已提交发送: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber());
            } catch (Exception notificationException) {
                // 通知发送失败不应该影响主流程
                logger.error("发送代码评审完成通知时发生错误: repo={}, pr={}", 
                    task.repositoryFullName(), task.prNumber(), notificationException);
            }
            
        } catch (Exception e) {
            logger.error("处理Graph工作流结果时发生错误: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber(), e);
        }
    }
    
    /**
     * 从各个Agent的结果构建最终的ReviewResultDTO
     */
    @SuppressWarnings("unchecked")
    private ReviewResultDTO buildReviewResultFromAgents(OverAllState state) {
        List<ReviewCommentDTO> allComments = new java.util.ArrayList<>();
        
        // 收集各个Agent的审查结果
        List<ReviewCommentDTO> styleIssues = (List<ReviewCommentDTO>) state.value("style_issues")
            .orElse(new java.util.ArrayList<>());
        List<ReviewCommentDTO> logicIssues = (List<ReviewCommentDTO>) state.value("logic_issues")
            .orElse(new java.util.ArrayList<>());
        List<ReviewCommentDTO> securityIssues = (List<ReviewCommentDTO>) state.value("security_issues")
            .orElse(new java.util.ArrayList<>());
        
        allComments.addAll(styleIssues);
        allComments.addAll(logicIssues);
        allComments.addAll(securityIssues);
        
        // 构建统计信息
        String summary = String.format("多智能体审查完成：发现 %d 个编码规范问题，%d 个逻辑问题，%d 个安全问题",
            styleIssues.size(), logicIssues.size(), securityIssues.size());
        
        // 计算整体评级
        String overallRating = calculateOverallRating(allComments.size());
        
        return new ReviewResultDTO(allComments, summary, overallRating);
    }
    
    /**
     * 根据问题数量计算整体评级
     */
    private String calculateOverallRating(int issueCount) {
        if (issueCount == 0) {
            return ReviewResultDTO.OverallRating.EXCELLENT.getValue();
        } else if (issueCount <= 3) {
            return ReviewResultDTO.OverallRating.GOOD.getValue();
        } else if (issueCount <= 10) {
            return ReviewResultDTO.OverallRating.NEEDS_IMPROVEMENT.getValue();
        } else {
            return ReviewResultDTO.OverallRating.POOR.getValue();
        }
    }
    
    /**
     * 构建审查通过的结果报告
     */
    private ReviewResultDTO buildApprovalResult(OverAllState state) {
        // 获取审查决策信息
        String triageDecision = (String) state.value("triage_decision").orElse("unknown");
        String changeType = (String) state.value("change_type").orElse("unknown");
        String prIntent = (String) state.value("pr_intent").orElse("代码变更");
        String markdownReport = (String) state.value("markdown_report").orElse("");
        
        // 构建审查通过的摘要
        String summary;
        if ("skip_review".equals(triageDecision) || "skip_detailed".equals(triageDecision)) {
            summary = String.format("✅ 代码审查完成\n\n" +
                "**变更类型**: %s\n" +
                "**PR意图**: %s\n\n" +
                "由于本次变更为%s类型，系统进行了快速审查，未发现明显问题。",
                changeType, prIntent, changeType);
        } else if (markdownReport != null && !markdownReport.isEmpty()) {
            // 如果有markdown报告，直接使用
            summary = markdownReport;
        } else {
            summary = String.format("✅ 代码审查通过\n\n" +
                "**变更类型**: %s\n" +
                "**PR意图**: %s\n\n" +
                "经过多智能体系统的全面审查，包括：\n" +
                "- 编码规范检查 ✓\n" +
                "- 逻辑与上下文分析 ✓\n" +
                "- 安全漏洞扫描 ✓\n\n" +
                "**结论**: 代码质量良好，未发现需要修改的问题。",
                changeType, prIntent);
        }
        
        // 创建一个空的评论列表（因为没有问题）
        List<ReviewCommentDTO> emptyComments = new java.util.ArrayList<>();
        
        // 返回审查通过的结果
        return new ReviewResultDTO(
            emptyComments,
            summary,
            ReviewResultDTO.OverallRating.EXCELLENT.getValue()
        );
    }
} 