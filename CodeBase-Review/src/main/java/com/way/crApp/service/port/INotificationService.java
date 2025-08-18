package com.way.crApp.service.port;

import com.way.crApp.dto.review.ReviewResultDTO;
import com.way.crApp.dto.review.ReviewTaskDTO;

/**
 * 通知服务接口
 * 支持多种通知方式的可扩展设计
 */
public interface INotificationService {

    /**
     * 发送代码评审完成通知
     *
     * @param task 审查任务信息
     * @param result 审查结果
     */
    void sendReviewCompletionNotification(ReviewTaskDTO task, ReviewResultDTO result);

    /**
     * 发送Wiki文档生成完成通知
     *
     * @param taskId 任务ID
     * @param repoName 仓库名称
     * @param documentCount 生成的文档数量
     */
    void sendWikiCompletionNotification(String taskId, String repoName, int documentCount);
}
