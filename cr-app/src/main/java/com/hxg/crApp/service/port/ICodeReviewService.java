package com.hxg.crApp.service.port;

import com.hxg.crApp.dto.review.ReviewTaskDTO;

/**
 * 代码审查服务接口
 * 
 * 定义审查的核心业务流程
 */
public interface ICodeReviewService {

    /**
     * 执行代码审查
     * 
     * @param task 审查任务
     */
    void performReview(ReviewTaskDTO task);
} 