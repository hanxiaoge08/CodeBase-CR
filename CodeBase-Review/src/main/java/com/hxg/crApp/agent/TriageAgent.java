package com.hxg.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 变更筛选员Agent - 进行PR初步评估和分流
 * 
 * 职责：
 * 1. 快速评估PR的大小和复杂度
 * 2. 检查PR标题和描述的规范性
 * 3. 判断是否需要详细审查
 * 4. 识别变更类型（文档、配置、代码等）
 * @author hxg
 */
@Component
public class TriageAgent implements NodeAction {
    
    private static final Logger logger = LoggerFactory.getLogger(TriageAgent.class);
    
    @Autowired
    private ChatClient chatClient;
    
    // 阈值配置
    private static final int MAX_DIFF_LINES = 1000;
    private static final int MAX_FILES_CHANGED = 20;
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("变更筛选员Agent开始执行");
        
        // 获取diff统计信息
        @SuppressWarnings("unchecked")
        Map<String, Object> diffStats = (Map<String, Object>) state.value("diff_stats").orElse(new HashMap<>());
        String prTitle = (String) state.value("pr_title").orElse("");
        String diffContent = (String) state.value("diff_content").orElse("");
        String[] changedFiles = (String[]) state.value("changed_files").orElse(new String[0]);
        
        Map<String, Object> result = new HashMap<>();
        
        // 1. 检查PR大小
        long totalChanges = (long) diffStats.getOrDefault("total_changes", 0L);
        long filesChanged = (long) diffStats.getOrDefault("files_changed", 0L);
        
        if (totalChanges > MAX_DIFF_LINES) {
            result.put("triage_decision", "too_large");
            result.put("triage_message", String.format(
                "PR过大（%d行变更），建议拆分为更小的PR", totalChanges));
            result.put("needs_detailed_review", false);
            logger.warn("PR过大: {} 行变更", totalChanges);
            return result;
        }
        
        if (filesChanged > MAX_FILES_CHANGED) {
            result.put("triage_decision", "too_many_files");
            result.put("triage_message", String.format(
                "修改文件过多（%d个），建议拆分PR", filesChanged));
            result.put("needs_detailed_review", false);
            logger.warn("修改文件过多: {} 个", filesChanged);
            return result;
        }
        
        // 2. 检查PR标题和描述规范性
        String titleCheck = checkPrTitle(prTitle);
        if (titleCheck != null) {
            result.put("triage_decision", "invalid_title");
            result.put("triage_message", titleCheck);
            result.put("needs_detailed_review", false);
            return result;
        }
        
        // 3. 识别变更类型
        String changeType = identifyChangeType(changedFiles, diffContent);
        result.put("change_type", changeType);
        
        // 4. 判断是否需要详细审查
        boolean needsDetailedReview = shouldPerformDetailedReview(changeType, totalChanges);
        result.put("needs_detailed_review", needsDetailedReview);
        
        // 5. 设置审查范围
        if (needsDetailedReview) {
            result.put("triage_decision", "proceed");
            result.put("review_scope", determineReviewScope(changeType));
            
            // 使用LLM分析PR意图
            String prIntent = analyzePrIntent(prTitle, diffContent);
            result.put("pr_intent", prIntent);
            
            logger.info("PR通过初步筛选，类型: {}, 需要详细审查", changeType);
        } else {
            result.put("triage_decision", "skip_detailed");
            result.put("triage_message", String.format(
                "变更类型为%s，无需详细代码审查", changeType));
            logger.info("PR类型: {}, 跳过详细审查", changeType);
        }
        
        return result;
    }
    
    /**
     * 检查PR标题规范性
     */
    private String checkPrTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "PR标题不能为空";
        }
        
        if (title.length() < 3) {  // 放宽限制从5改为3
            return "PR标题过短，请提供更详细的描述";
        }
        
        if (title.length() > 100) {
            return "PR标题过长，请精简到100字符以内";
        }
        
        // 检查是否符合约定格式，如: feat:, fix:, docs:, refactor:等
        // 但这只是建议，不是强制要求
        String[] prefixes = {"feat:", "fix:", "docs:", "style:", "refactor:", 
                            "test:", "chore:", "perf:", "build:", "ci:"};
        boolean hasPrefix = false;
        for (String prefix : prefixes) {
            if (title.toLowerCase().startsWith(prefix)) {
                hasPrefix = true;
                break;
            }
        }
        
        if (!hasPrefix) {
            // 只记录警告，不阻止审查
            logger.info("PR标题建议以类型前缀开头(如: feat:, fix:, docs:等)，当前标题: {}", title);
        }
        
        // 标题符合基本要求就通过
        return null; 
    }
    
    /**
     * 识别变更类型
     */
    private String identifyChangeType(String[] changedFiles, String diffContent) {
        boolean hasCode = false;
        boolean hasDocs = false;
        boolean hasConfig = false;
        boolean hasTests = false;
        
        for (String file : changedFiles) {
            if (file.endsWith(".java") || file.endsWith(".js") || file.endsWith(".py") 
                || file.endsWith(".go") || file.endsWith(".cpp")) {
                hasCode = true;
            } else if (file.endsWith(".md") || file.endsWith(".txt") || file.contains("/docs/")) {
                hasDocs = true;
            } else if (file.endsWith(".yml") || file.endsWith(".yaml") || file.endsWith(".xml") 
                      || file.endsWith(".properties") || file.endsWith(".json")) {
                hasConfig = true;
            } else if (file.contains("/test/") || file.contains("Test.java") || file.contains("_test.")) {
                hasTests = true;
            }
        }
        
        if (hasCode && !hasDocs && !hasConfig) {
            return "code_only";
        } else if (hasDocs && !hasCode) {
            return "documentation";
        } else if (hasConfig && !hasCode) {
            return "configuration";
        } else if (hasTests && !hasCode) {
            return "test_only";
        } else if (hasCode) {
            return "mixed";
        } else {
            return "other";
        }
    }
    
    /**
     * 判断是否需要详细审查
     */
    private boolean shouldPerformDetailedReview(String changeType, long totalChanges) {
        // 纯文档和配置变更不需要详细代码审查
        if ("documentation".equals(changeType) || "configuration".equals(changeType)) {
            return false;
        }
        
        // 只要有代码变更就需要详细审查（移除了对变更行数的限制）
        // 即使是小的代码变更也可能包含重要问题
        if ("code_only".equals(changeType) || "mixed".equals(changeType) || "test_only".equals(changeType)) {
            return true;
        }
        
        // 对于其他类型，如果变更较大也需要审查
        return totalChanges >= 5;
    }
    
    /**
     * 确定审查范围
     */
    private String[] determineReviewScope(String changeType) {
        return switch (changeType) {
            case "code_only" -> new String[]{"style", "logic", "security", "performance"};
            case "mixed" -> new String[]{"style", "logic", "security"};
            case "test_only" -> new String[]{"test_coverage", "test_quality"};
            default -> new String[]{"general"};
        };
    }
    
    /**
     * 使用LLM分析PR意图
     */
    private String analyzePrIntent(String prTitle, String diffContent) {
        try {
            // 只取前500行diff进行分析，避免token过多
            String diffSample = diffContent.lines()
                .limit(500)
                .reduce("", (a, b) -> a + "\n" + b);
            
            String prompt = String.format(
                    """
                            基于PR标题和代码变更，简要分析这个PR的主要目的(不超过50字):
                            标题: %s
                            变更示例:
                            %s""",
                prTitle, diffSample
            );
            
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        } catch (Exception e) {
            logger.error("分析PR意图失败", e);
            return "无法分析PR意图";
        }
    }
}