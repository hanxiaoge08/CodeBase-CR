package com.way.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 并行审查启动器 - 启动三个专家Agent并行执行
 * 
 * 职责：
 * 1. 作为并行执行的入口点
 * 2. 准备并行执行所需的状态数据
 * 3. 触发三个专家Agent的并行执行
 */
@Component
public class ParallelReviewStarter implements NodeAction {
    
    private static final Logger logger = LoggerFactory.getLogger(ParallelReviewStarter.class);
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("并行审查启动器开始执行，准备启动三个专家Agent");
        
        // 记录并行审查开始时间
        Map<String, Object> result = new HashMap<>();
        result.put("parallel_review_started", System.currentTimeMillis());
        result.put("parallel_review_status", "started");
        
        String repoName = (String) state.value("repo_name").orElse("unknown");
        String prTitle = (String) state.value("pr_title").orElse("unknown");
        
        logger.info("启动并行审查: repo={}, pr={}", repoName, prTitle);
        
        return result;
    }
}