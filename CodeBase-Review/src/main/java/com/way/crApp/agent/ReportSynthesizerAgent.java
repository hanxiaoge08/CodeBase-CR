package com.way.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.way.crApp.dto.review.ReviewCommentDTO;
import com.way.crApp.dto.review.ReviewResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告综合与发布员Agent - 汇总格式化审查结果
 * 
 * 职责：
 * 1. 汇总所有专家Agent的审查结果
 * 2. 去重和过滤相似问题
 * 3. 按类别和严重程度分类排序
 * 4. 语言润色，生成友好的建设性评论
 * 5. 格式化为GitHub PR适用的Markdown报告
 */
@Component
public class ReportSynthesizerAgent implements NodeAction {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportSynthesizerAgent.class);
    
    @Autowired
    private ChatClient chatClient;
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("报告综合与发布员Agent开始执行");
        
        // 收集所有审查结果
        List<ReviewCommentDTO> allIssues = collectAllIssues(state);
        
        // 去重和过滤
        List<ReviewCommentDTO> filteredIssues = deduplicateAndFilter(allIssues);
        
        // 分类和排序
        Map<String, List<ReviewCommentDTO>> categorizedIssues = categorizeIssues(filteredIssues);
        
        // 生成统计信息
        Map<String, Object> statistics = generateStatistics(filteredIssues);
        
        // 计算整体评级
        String overallRating = calculateOverallRating(filteredIssues);
        
        // 生成审查总结
        String summary = generateSummary(statistics, overallRating);
        
        // 润色评论内容
        List<ReviewCommentDTO> polishedIssues = polishComments(filteredIssues);
        
        // 生成最终报告
        String markdownReport = generateMarkdownReport(
            categorizedIssues, statistics, overallRating, summary);
        
        // 构建最终结果
        ReviewResultDTO result = ReviewResultDTO.builder()
            .comments(polishedIssues)
            .summary(summary)
            .overallRating(overallRating)
            .build();
        
        Map<String, Object> output = new HashMap<>();
        output.put("final_review_result", result);
        output.put("markdown_report", markdownReport);
        output.put("issue_statistics", statistics);
        output.put("total_issues", filteredIssues.size());
        
        logger.info("报告综合完成，生成 {} 个问题，整体评级: {}", 
            filteredIssues.size(), overallRating);
        
        return output;
    }
    
    /**
     * 收集所有Agent的审查结果
     */
    @SuppressWarnings("unchecked")
    private List<ReviewCommentDTO> collectAllIssues(OverAllState state) {
        List<ReviewCommentDTO> allIssues = new ArrayList<>();
        
        // 从编码规范Agent收集问题
        List<ReviewCommentDTO> styleIssues = 
            (List<ReviewCommentDTO>) state.value("style_issues").orElse(new ArrayList<>());
        allIssues.addAll(styleIssues);
        
        // 从逻辑上下文Agent收集问题
        List<ReviewCommentDTO> logicIssues = 
            (List<ReviewCommentDTO>) state.value("logic_issues").orElse(new ArrayList<>());
        allIssues.addAll(logicIssues);
        
        // 从安全扫描Agent收集问题
        List<ReviewCommentDTO> securityIssues = 
            (List<ReviewCommentDTO>) state.value("security_issues").orElse(new ArrayList<>());
        allIssues.addAll(securityIssues);
        
        logger.debug("收集到审查问题: 规范={}, 逻辑={}, 安全={}", 
            styleIssues.size(), logicIssues.size(), securityIssues.size());
        
        return allIssues;
    }
    
    /**
     * 去重和过滤相似问题
     */
    private List<ReviewCommentDTO> deduplicateAndFilter(List<ReviewCommentDTO> issues) {
        // 按文件和行号分组，合并相似问题
        Map<String, List<ReviewCommentDTO>> groupedByLocation = issues.stream()
            .collect(Collectors.groupingBy(
                issue -> issue.filePath() + ":" + issue.lineNumber()));
        
        List<ReviewCommentDTO> filteredIssues = new ArrayList<>();
        
        for (List<ReviewCommentDTO> locationIssues : groupedByLocation.values()) {
            if (locationIssues.size() == 1) {
                filteredIssues.add(locationIssues.get(0));
            } else {
                // 合并同一位置的多个问题
                ReviewCommentDTO merged = mergeIssuesAtSameLocation(locationIssues);
                filteredIssues.add(merged);
            }
        }
        
        // 过滤掉优先级过低的问题（如果总问题数过多）
        if (filteredIssues.size() > 50) {
            filteredIssues = filteredIssues.stream()
                .filter(issue -> !"info".equals(issue.severity()))
                .collect(Collectors.toList());
        }
        
        if (filteredIssues.size() > 30) {
            filteredIssues = filteredIssues.stream()
                .filter(issue -> "error".equals(issue.severity()))
                .collect(Collectors.toList());
        }
        
        return filteredIssues;
    }
    
    /**
     * 合并同一位置的多个问题
     */
    private ReviewCommentDTO mergeIssuesAtSameLocation(List<ReviewCommentDTO> issues) {
        // 选择最高严重程度
        String highestSeverity = issues.stream()
            .map(ReviewCommentDTO::severity)
            .max(Comparator.comparing(this::getSeverityWeight))
            .orElse("info");
        
        // 合并评论内容
        String mergedComment = issues.stream()
            .map(ReviewCommentDTO::comment)
            .distinct()
            .collect(Collectors.joining("; "));
        
        // 使用第一个问题作为模板
        ReviewCommentDTO first = issues.get(0);
        return ReviewCommentDTO.builder()
            .filePath(first.filePath())
            .lineNumber(first.lineNumber())
            .comment(mergedComment)
            .severity(highestSeverity)
            .build();
    }
    
    /**
     * 获取严重程度权重
     */
    private int getSeverityWeight(String severity) {
        return switch (severity) {
            case "error" -> 3;
            case "warning" -> 2;
            case "info" -> 1;
            default -> 0;
        };
    }
    
    /**
     * 按类别分类问题
     */
    private Map<String, List<ReviewCommentDTO>> categorizeIssues(List<ReviewCommentDTO> issues) {
        Map<String, List<ReviewCommentDTO>> categorized = new LinkedHashMap<>();
        
        // 按严重程度分类
        categorized.put("🚨 严重问题", 
            issues.stream().filter(i -> "error".equals(i.severity())).collect(Collectors.toList()));
        categorized.put("⚠️ 警告", 
            issues.stream().filter(i -> "warning".equals(i.severity())).collect(Collectors.toList()));
        categorized.put("💡 建议", 
            issues.stream().filter(i -> "info".equals(i.severity())).collect(Collectors.toList()));
        
        // 移除空分类
        categorized.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        return categorized;
    }
    
    /**
     * 生成统计信息
     */
    private Map<String, Object> generateStatistics(List<ReviewCommentDTO> issues) {
        Map<String, Object> stats = new HashMap<>();
        
        long errorCount = issues.stream().filter(i -> "error".equals(i.severity())).count();
        long warningCount = issues.stream().filter(i -> "warning".equals(i.severity())).count();
        long infoCount = issues.stream().filter(i -> "info".equals(i.severity())).count();
        
        stats.put("error_count", errorCount);
        stats.put("warning_count", warningCount);
        stats.put("info_count", infoCount);
        stats.put("total_count", issues.size());
        
        // 按文件统计
        Map<String, Long> fileStats = issues.stream()
            .collect(Collectors.groupingBy(
                ReviewCommentDTO::filePath,
                Collectors.counting()));
        stats.put("files_with_issues", fileStats.size());
        stats.put("most_issues_file", 
            fileStats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无"));
        
        return stats;
    }
    
    /**
     * 计算整体评级
     */
    private String calculateOverallRating(List<ReviewCommentDTO> issues) {
        long errorCount = issues.stream().filter(i -> "error".equals(i.severity())).count();
        long warningCount = issues.stream().filter(i -> "warning".equals(i.severity())).count();
        long totalCount = issues.size();
        
        if (errorCount > 0) {
            if (errorCount >= 5) {
                return "poor";
            } else {
                return "needs_improvement";
            }
        } else if (warningCount > 0) {
            if (warningCount >= 10) {
                return "needs_improvement";
            } else {
                return "good";
            }
        } else if (totalCount > 0) {
            return "good";
        } else {
            return "excellent";
        }
    }
    
    /**
     * 生成审查总结
     */
    private String generateSummary(Map<String, Object> statistics, String overallRating) {
        try {
            String prompt = String.format(
                "基于代码审查统计信息，生成一段专业的审查总结(不超过100字):\n" +
                "- 错误: %d个\n" +
                "- 警告: %d个\n" +
                "- 建议: %d个\n" +
                "- 整体评级: %s\n" +
                "- 涉及文件: %d个\n\n" +
                "要求：语言友好建设性，避免过于严厉",
                statistics.get("error_count"),
                statistics.get("warning_count"),
                statistics.get("info_count"),
                overallRating,
                statistics.get("files_with_issues")
            );
            
            return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
                
        } catch (Exception e) {
            logger.error("生成审查总结失败", e);
            return generateDefaultSummary(statistics);
        }
    }
    
    /**
     * 生成默认总结
     */
    private String generateDefaultSummary(Map<String, Object> statistics) {
        long errorCount = (Long) statistics.get("error_count");
        long warningCount = (Long) statistics.get("warning_count");
        long infoCount = (Long) statistics.get("info_count");
        
        if (errorCount == 0 && warningCount == 0 && infoCount == 0) {
            return "代码质量良好，未发现明显问题，可以合并。";
        } else if (errorCount == 0) {
            return String.format("代码整体质量不错，有%d个改进建议，建议优化后合并。", 
                warningCount + infoCount);
        } else {
            return String.format("发现%d个需要修复的问题，建议修复后重新提交。", errorCount);
        }
    }
    
    /**
     * 润色评论内容
     */
    private List<ReviewCommentDTO> polishComments(List<ReviewCommentDTO> issues) {
        // 限制评论长度，优化表达
        return issues.stream()
            .map(this::polishSingleComment)
            .collect(Collectors.toList());
    }
    
    /**
     * 润色单个评论
     */
    private ReviewCommentDTO polishSingleComment(ReviewCommentDTO issue) {
        String originalComment = issue.comment();
        String polishedComment = originalComment;
        
        // 添加友好的开头
        if ("error".equals(issue.severity())) {
            if (!originalComment.startsWith("❌") && !originalComment.startsWith("🚨")) {
                polishedComment = "🔧 " + polishedComment;
            }
        } else if ("warning".equals(issue.severity())) {
            if (!originalComment.startsWith("⚠️") && !originalComment.startsWith("💡")) {
                polishedComment = "💡 " + polishedComment;
            }
        }
        
        // 确保评论不超过200字符
        if (polishedComment.length() > 200) {
            polishedComment = polishedComment.substring(0, 197) + "...";
        }
        
        return ReviewCommentDTO.builder()
            .filePath(issue.filePath())
            .lineNumber(issue.lineNumber())
            .comment(polishedComment)
            .severity(issue.severity())
            .build();
    }
    
    /**
     * 生成Markdown格式报告
     */
    private String generateMarkdownReport(Map<String, List<ReviewCommentDTO>> categorizedIssues,
                                          Map<String, Object> statistics,
                                          String overallRating,
                                          String summary) {
        StringBuilder report = new StringBuilder();
        
        // 报告头部
        report.append("## 🤖 AI代码审查报告\n\n");
        report.append("**生成时间：** ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // 整体评级
        String ratingEmoji = getRatingEmoji(overallRating);
        report.append("### ").append(ratingEmoji).append(" 整体评级：").append(translateRating(overallRating)).append("\n\n");
        
        // 审查总结
        report.append("**审查总结：**\n");
        report.append(summary).append("\n\n");
        
        // 统计信息
        report.append("### 📊 问题统计\n\n");
        report.append("| 类型 | 数量 |\n");
        report.append("|------|------|\n");
        report.append("| 🚨 严重问题 | ").append(statistics.get("error_count")).append(" |\n");
        report.append("| ⚠️ 警告 | ").append(statistics.get("warning_count")).append(" |\n");
        report.append("| 💡 建议 | ").append(statistics.get("info_count")).append(" |\n");
        report.append("| 📁 涉及文件 | ").append(statistics.get("files_with_issues")).append(" |\n\n");
        
        // 详细问题列表
        if (!categorizedIssues.isEmpty()) {
            report.append("### 📝 详细问题列表\n\n");
            
            for (Map.Entry<String, List<ReviewCommentDTO>> entry : categorizedIssues.entrySet()) {
                String category = entry.getKey();
                List<ReviewCommentDTO> issues = entry.getValue();
                
                if (!issues.isEmpty()) {
                    report.append("#### ").append(category).append(" (").append(issues.size()).append("个)\n\n");
                    
                    for (int i = 0; i < Math.min(issues.size(), 10); i++) { // 限制显示数量
                        ReviewCommentDTO issue = issues.get(i);
                        report.append("**").append(issue.filePath()).append(":").append(issue.lineNumber()).append("**\n");
                        report.append("> ").append(issue.comment()).append("\n\n");
                    }
                    
                    if (issues.size() > 10) {
                        report.append("*... 还有 ").append(issues.size() - 10).append(" 个类似问题*\n\n");
                    }
                }
            }
        } else {
            report.append("### ✅ 未发现代码问题\n\n");
            report.append("恭喜！代码质量良好，未发现明显问题。\n\n");
        }
        
        // 报告尾部
        report.append("---\n");
        report.append("*本报告由 CodeBase-CR AI代码审查系统自动生成*\n");
        report.append("*审查时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("*");
        
        return report.toString();
    }
    
    /**
     * 获取评级对应的emoji
     */
    private String getRatingEmoji(String rating) {
        return switch (rating) {
            case "excellent" -> "🌟";
            case "good" -> "✅";
            case "needs_improvement" -> "⚠️";
            case "poor" -> "❌";
            default -> "📝";
        };
    }
    
    /**
     * 翻译评级
     */
    private String translateRating(String rating) {
        return switch (rating) {
            case "excellent" -> "优秀";
            case "good" -> "良好";
            case "needs_improvement" -> "需要改进";
            case "poor" -> "较差";
            default -> "未评级";
        };
    }
}