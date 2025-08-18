package com.way.service;

/**
 * 通知服务接口
 * 支持多种通知方式的可扩展设计
 */
public interface INotificationService {

    /**
     * 发送Wiki文档生成完成通知
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param documentCount 生成的文档数量
     */
    void sendWikiCompletionNotification(String taskId, String repoName, int documentCount);

    /**
     * 发送Wiki任务处理失败通知
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param errorMessage 错误信息
     */
    void sendWikiFailureNotification(String taskId, String repoName, String errorMessage);
}
