package com.way.service;

/**
 * 任务进度管理服务接口
 * 基于Redis实现任务进度跟踪和幂等性控制
 */
public interface ITaskProgressService {

    /**
     * 设置任务总数
     *
     * @param taskId 任务ID
     * @param totalCount 总任务数
     */
    void setTaskTotal(String taskId, long totalCount);

    /**
     * 增加已消费任务数（原子操作）
     *
     * @param taskId 任务ID
     * @return 增加后的消费数量
     */
    long incrementConsumedCount(String taskId);

    /**
     * 获取任务总数
     *
     * @param taskId 任务ID
     * @return 任务总数，不存在返回0
     */
    long getTaskTotal(String taskId);

    /**
     * 获取已消费任务数
     *
     * @param taskId 任务ID
     * @return 已消费任务数，不存在返回0
     */
    long getConsumedCount(String taskId);

    /**
     * 检查任务是否完成
     *
     * @param taskId 任务ID
     * @return 是否完成
     */
    boolean isTaskCompleted(String taskId);

    /**
     * 清理任务进度数据
     *
     * @param taskId 任务ID
     */
    void clearTaskProgress(String taskId);

    /**
     * 检查和处理消息幂等性
     * 使用catalogueId作为幂等键
     *
     * @param catalogueId 目录ID（幂等键）
     * @return 幂等处理结果
     */
    IdempotentResult checkIdempotent(String catalogueId);

    /**
     * 标记消息处理完成
     *
     * @param catalogueId 目录ID
     */
    void markMessageCompleted(String catalogueId);

    /**
     * 幂等处理结果枚举
     */
    enum IdempotentResult {
        FIRST_TIME,     // 第一次处理
        PROCESSING,     // 正在处理中
        COMPLETED       // 已处理完成
    }
}
