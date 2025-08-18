package com.way.service.notification;

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
     * 发送Wiki文档生成完成通知
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param documentCount 生成的文档数量
     */
    public abstract void sendWikiNotification(String taskId, String repoName, int documentCount);

    /**
     * 发送Wiki任务处理失败通知
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param errorMessage 错误信息
     */
    public abstract void sendWikiFailureNotification(String taskId, String repoName, String errorMessage);

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
     * 构建Wiki生成完成通知消息
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param documentCount 文档数量
     * @return 格式化的消息内容
     */
    protected String buildWikiCompletionMessage(String taskId, String repoName, int documentCount) {
        StringBuilder message = new StringBuilder();
        message.append("📚 Wiki文档生成完成通知\n\n");
        message.append("📂 仓库：").append(repoName).append("\n");
        message.append("🆔 任务ID：").append(taskId).append("\n");
        message.append("📄 生成文档：").append(documentCount).append(" 个\n");
        message.append("✅ 状态：已完成\n");
        message.append("⏰ 完成时间：").append(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        return message.toString();
    }

    /**
     * 构建Wiki任务失败通知消息
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param errorMessage 错误信息
     * @return 格式化的消息内容
     */
    protected String buildWikiFailureMessage(String taskId, String repoName, String errorMessage) {
        StringBuilder message = new StringBuilder();
        message.append("❌ Wiki文档生成失败通知\n\n");
        message.append("📂 仓库：").append(repoName).append("\n");
        message.append("🆔 任务ID：").append(taskId).append("\n");
        message.append("❌ 状态：处理失败\n");
        message.append("🔍 错误信息：").append(errorMessage).append("\n");
        message.append("⏰ 失败时间：").append(java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        return message.toString();
    }
}
