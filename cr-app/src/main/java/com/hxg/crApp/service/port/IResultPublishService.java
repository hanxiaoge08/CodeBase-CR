package com.hxg.crApp.service.port;

import com.hxg.crApp.dto.review.ReviewResultDTO;
import com.hxg.crApp.dto.review.ReviewTaskDTO;

/**
 * 结果发布服务接口
 * 
 * 定义审查结果发布逻辑
 */
public interface IResultPublishService {

    /**
     * 发布审查结果到GitHub PR
     * 
     * @param task 审查任务
     * @param result 审查结果
     */
    void publishReviewResult(ReviewTaskDTO task, ReviewResultDTO result);
} 