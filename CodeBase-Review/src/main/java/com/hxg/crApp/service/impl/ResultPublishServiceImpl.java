package com.hxg.crApp.service.impl;

import com.hxg.crApp.adapter.github.GitHubAdapter;
import com.hxg.crApp.dto.review.ReviewCommentDTO;
import com.hxg.crApp.dto.review.ReviewResultDTO;
import com.hxg.crApp.dto.review.ReviewTaskDTO;
import com.hxg.crApp.service.port.IResultPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 结果发布服务实现
 * 
 * 将格式化的审查意见通过 GitHub API 发布到 PR
 */
@Service
public class ResultPublishServiceImpl implements IResultPublishService {

    private static final Logger logger = LoggerFactory.getLogger(ResultPublishServiceImpl.class);

    @Autowired
    private GitHubAdapter gitHubAdapter;

    @Override
    public void publishReviewResult(ReviewTaskDTO task, ReviewResultDTO result) {
        try {
            logger.info("开始发布审查结果: repo={}, pr={}, 评论数={}", 
                task.repositoryFullName(), task.prNumber(), 
                result.comments() != null ? result.comments().size() : 0);

            // 发布总体评审总结
            if (result.summary() != null && !result.summary().trim().isEmpty()) {
                String summaryComment = buildSummaryComment(result);
                gitHubAdapter.publishGeneralComment(
                    task.repositoryFullName(), 
                    task.prNumber(), 
                    summaryComment
                );
            }

            // 发布具体的行级评论
            if (result.comments() != null && !result.comments().isEmpty()) {
                publishLineComments(task, result.comments());
            }

            logger.info("审查结果发布完成: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber());

        } catch (Exception e) {
            logger.error("发布审查结果时发生错误: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber(), e);
        }
    }

    /**
     * 发布行级评论
     * 
     * @param task 审查任务
     * @param comments 评论列表
     */
    private void publishLineComments(ReviewTaskDTO task, java.util.List<ReviewCommentDTO> comments) {
        int successCount = 0;
        int errorCount = 0;

        for (ReviewCommentDTO comment : comments) {
            try {
                // 为评论添加严重程度标识
                String formattedComment = formatComment(comment);
                
                ReviewCommentDTO formattedCommentDto = ReviewCommentDTO.builder()
                    .filePath(comment.filePath())
                    .lineNumber(comment.lineNumber())
                    .comment(formattedComment)
                    .severity(comment.severity())
                    .build();

                gitHubAdapter.publishLineComment(
                    task.repositoryFullName(), 
                    task.prNumber(), 
                    formattedCommentDto
                );
                
                successCount++;
                
                // 添加延迟避免API速率限制
                Thread.sleep(100);
                
            } catch (Exception e) {
                logger.error("发布行级评论失败: file={}, line={}, error={}", 
                    comment.filePath(), comment.lineNumber(), e.getMessage());
                errorCount++;
            }
        }

        logger.info("行级评论发布完成: 成功={}, 失败={}", successCount, errorCount);
    }

    /**
     * 构建总结评论
     * 
     * @param result 审查结果
     * @return 格式化的总结评论
     */
    private String buildSummaryComment(ReviewResultDTO result) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("## 🤖 AI代码审查报告\n\n");
        
        // 整体评级
        builder.append("**整体评级:** ")
            .append(getRatingEmoji(result.overallRating()))
            .append(" ")
            .append(result.overallRating())
            .append("\n\n");
        
        // 审查总结
        builder.append("**审查总结:**\n")
            .append(result.summary())
            .append("\n\n");
        
        // 统计信息
        if (result.comments() != null && !result.comments().isEmpty()) {
            long errorCount = result.comments().stream()
                .filter(c -> "error".equals(c.severity()))
                .count();
            long warningCount = result.comments().stream()
                .filter(c -> "warning".equals(c.severity()))
                .count();
            long infoCount = result.comments().stream()
                .filter(c -> "info".equals(c.severity()))
                .count();
            
            builder.append("**问题统计:**\n");
            if (errorCount > 0) {
                builder.append("- ❌ 错误: ").append(errorCount).append("\n");
            }
            if (warningCount > 0) {
                builder.append("- ⚠️ 警告: ").append(warningCount).append("\n");
            }
            if (infoCount > 0) {
                builder.append("- ℹ️ 信息: ").append(infoCount).append("\n");
            }
        }
        
        builder.append("\n---\n")
            .append("*由 CodeBase-CR 自动生成*");
        
        return builder.toString();
    }

    /**
     * 格式化单条评论
     * 
     * @param comment 评论
     * @return 格式化后的评论
     */
    private String formatComment(ReviewCommentDTO comment) {
        String emoji = getSeverityEmoji(comment.severity());
        return String.format("%s **[%s]** %s", 
            emoji, 
            comment.severity().toUpperCase(), 
            comment.comment());
    }

    /**
     * 获取评级对应的表情符号
     * 
     * @param rating 评级
     * @return 表情符号
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
     * 获取严重程度对应的表情符号
     * 
     * @param severity 严重程度
     * @return 表情符号
     */
    private String getSeverityEmoji(String severity) {
        return switch (severity) {
            case "error" -> "❌";
            case "warning" -> "⚠️";
            case "info" -> "ℹ️";
            default -> "📝";
        };
    }
} 