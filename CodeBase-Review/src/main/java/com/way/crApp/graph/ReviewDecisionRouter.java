package com.way.crApp.graph;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审查决策路由器 - 根据Triage结果决定工作流下一步
 * 
 * 职责：
 * 1. 根据TriageAgent的决策结果路由到不同的分支
 * 2. 支持跳过详细审查、进行详细审查、PR过大结束、无效PR结束等路径
 * 3. 确保工作流按预期执行
 */
@Component
public class ReviewDecisionRouter implements EdgeAction {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewDecisionRouter.class);
    
    @Override
    public String apply(OverAllState state) throws Exception {
        try {
            // 获取triage决策结果
            String triageDecision = (String) state.value("triage_decision").orElse("detailed_review");
            Boolean needsDetailedReview = (Boolean) state.value("needs_detailed_review").orElse(true);
            String changeType = (String) state.value("change_type").orElse("code");
            
            logger.info("决策路由: triage_decision={}, needs_detailed_review={}, change_type={}", 
                triageDecision, needsDetailedReview, changeType);
            
            // 1. 检查是否为无效标题（现在不应该发生了，但保留以防万一）
            if ("invalid_title".equals(triageDecision)) {
                logger.info("路由决策: PR标题不规范，但继续审查");
                // 不再因为标题问题就跳过审查
                return needsDetailedReview ? "detailed_review" : "skip_review";
            }
            
            // 2. 检查是否为无效PR
            if ("invalid".equals(triageDecision)) {
                logger.info("路由决策: PR无效，直接结束");
                return "invalid";
            }
            
            // 3. 检查PR是否过大
            if ("too_large".equals(triageDecision) || "too_many_files".equals(triageDecision)) {
                logger.info("路由决策: PR过大或文件过多，直接结束");
                return "too_large";
            }
            
            // 4. 根据triage的proceed决策
            if ("proceed".equals(triageDecision)) {
                logger.info("路由决策: Triage通过，进行详细审查");
                return "detailed_review";
            }
            
            // 5. 跳过详细审查的情况
            if ("skip_detailed".equals(triageDecision) || "skip_review".equals(triageDecision)) {
                logger.info("路由决策: 跳过详细审查，直接生成报告");
                return "skip_review";
            }
            
            // 6. 根据needsDetailedReview标志决定
            if (!needsDetailedReview) {
                logger.info("路由决策: 不需要详细审查，直接生成报告");
                return "skip_review";
            }
            
            // 7. 根据变更类型决定
            if ("documentation".equals(changeType) || "configuration".equals(changeType)) {
                logger.info("路由决策: {}变更，跳过详细审查", changeType);
                return "skip_review";
            }
            
            // 8. 默认进行详细审查
            logger.info("路由决策: 默认进行详细审查");
            return "detailed_review";
            
        } catch (Exception e) {
            logger.error("决策路由过程中发生异常", e);
            // 异常情况下默认进行详细审查，确保不遗漏重要问题
            return "detailed_review";
        }
    }
    
    /**
     * 获取路由决策的可读描述
     */
    public String getDecisionDescription(String decision) {
        return switch (decision) {
            case "detailed_review" -> "进行详细审查";
            case "skip_review" -> "跳过详细审查";
            case "too_large" -> "PR过大，直接结束";
            case "invalid" -> "PR无效，直接结束";
            default -> "未知决策: " + decision;
        };
    }
    
    /**
     * 验证决策结果是否有效
     */
    public boolean isValidDecision(String decision) {
        return "detailed_review".equals(decision) || 
               "skip_review".equals(decision) || 
               "too_large".equals(decision) || 
               "invalid".equals(decision);
    }
}