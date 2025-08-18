package com.way.crApp.service.notification;

import com.way.crApp.dto.review.ReviewResultDTO;
import com.way.crApp.dto.review.ReviewTaskDTO;

/**
 * 通知渠道抽象基类
 * 提供可扩展的通知渠道实现框架
 */
public abstract class NotificationChannel {

    protected final String channelName;

    protected NotificationChannel(String channelName) {
        this.channelName = channelName;
    }

    /**
     * 发送代码评审完成通知
     *
     * @param task 审查任务信息
     * @param result 审查结果
     */
    public abstract void sendReviewNotification(ReviewTaskDTO task, ReviewResultDTO result);

    /**
     * 发送Wiki文档生成完成通知
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param documentCount 生成的文档数量
     */
    public abstract void sendWikiNotification(String taskId, String repoName, int documentCount);

    /**
     * 检查通知渠道是否启用
     *
     * @return 是否启用
     */
    public abstract boolean isEnabled();

    /**
     * 获取通知渠道名称
     *
     * @return 渠道名称
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * 构建代码评审通知消息
     *
     * @param task 审查任务
     * @param result 审查结果
     * @return 格式化的消息内容
     */
    protected String buildReviewMessage(ReviewTaskDTO task, ReviewResultDTO result) {
        StringBuilder message = new StringBuilder();
        message.append("🔍 代码评审完成通知\n\n");
        message.append("📂 仓库：").append(task.repositoryFullName()).append("\n");
        message.append("🔗 PR #").append(task.prNumber()).append("：").append(task.prTitle()).append("\n");
        message.append("👤 作者：").append(task.prAuthor()).append("\n");
        message.append("⭐ 评级：").append(result.overallRating()).append("\n");

        if (result.comments() != null && !result.comments().isEmpty()) {
            message.append("📝 发现问题：").append(result.comments().size()).append(" 个\n");
        } else {
            message.append("✅ 未发现问题\n");
        }

        message.append("\n📊 审查摘要：\n");
        message.append(result.summary());

        return message.toString();
    }

    /**
     * 构建Wiki生成完成通知消息
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param documentCount 文档数量
     * @return 格式化的消息内容
     */
    protected String buildWikiMessage(String taskId, String repoName, int documentCount) {
        StringBuilder message = new StringBuilder();
        message.append("📚 Wiki文档生成完成通知\n\n");
        message.append("📂 仓库：").append(repoName).append("\n");
        message.append("🆔 任务ID：").append(taskId).append("\n");
        message.append("📄 生成文档：").append(documentCount).append(" 个\n");
        message.append("✅ 状态：已完成\n");
        
        return message.toString();
    }
}
