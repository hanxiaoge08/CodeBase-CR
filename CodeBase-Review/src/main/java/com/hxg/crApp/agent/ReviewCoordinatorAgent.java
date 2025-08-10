package com.hxg.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.hxg.crApp.dto.review.ReviewTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 审查协调员Agent - 作为整个审查流程的入口和控制中心
 * 
 * 职责：
 * 1. 接收代码审查任务
 * 2. 解析PR元数据和diff内容
 * 3. 协调其他Agent的执行
 * 4. 管理整体流程状态
 */
@Component
public class ReviewCoordinatorAgent implements NodeAction {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewCoordinatorAgent.class);
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("审查协调员Agent开始执行");
        
        // 从状态中获取审查任务
        ReviewTaskDTO task = (ReviewTaskDTO) state.value("review_task").orElseThrow(
            () -> new IllegalStateException("未找到审查任务")
        );
        
        String diffContent = (String) state.value("diff_content").orElseThrow(
            () -> new IllegalStateException("未找到diff内容")
        );
        
        logger.info("开始处理PR审查任务: repo={}, pr={}, title={}", 
            task.repositoryFullName(), task.prNumber(), task.prTitle());
        
        Map<String, Object> result = new HashMap<>();
        
        // 准备基础数据供其他Agent使用
        result.put("repo_name", task.repositoryFullName());
        result.put("pr_number", task.prNumber());
        result.put("pr_title", task.prTitle());
        result.put("pr_author", task.prAuthor());
        result.put("diff_content", diffContent);
        result.put("base_sha", task.baseSha());
        result.put("head_sha", task.headSha());
        
        // 解析diff获取变更文件列表
        result.put("changed_files", extractChangedFiles(diffContent));
        
        // 计算diff统计信息
        Map<String, Object> diffStats = calculateDiffStats(diffContent);
        result.put("diff_stats", diffStats);
        
        // 设置审查配置
        Map<String, Object> reviewConfig = new HashMap<>();
        reviewConfig.put("max_issues_per_file", 10);
        reviewConfig.put("enable_security_scan", true);
        reviewConfig.put("enable_performance_check", true);
        result.put("review_config", reviewConfig);
        
        logger.info("审查协调员Agent完成任务准备，diff统计: {}", diffStats);
        
        return result;
    }
    
    /**
     * 从diff内容中提取变更文件列表
     */
    private String[] extractChangedFiles(String diffContent) {
        return diffContent.lines()
            .filter(line -> line.startsWith("diff --git"))
            .map(line -> {
                String[] parts = line.split(" ");
                if (parts.length >= 4) {
                    return parts[3].substring(2); // 去掉 b/ 前缀
                }
                return "";
            })
            .filter(file -> !file.isEmpty())
            .toArray(String[]::new);
    }
    
    /**
     * 计算diff统计信息
     */
    private Map<String, Object> calculateDiffStats(String diffContent) {
        Map<String, Object> stats = new HashMap<>();
        
        long linesAdded = diffContent.lines()
            .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
            .count();
            
        long linesDeleted = diffContent.lines()
            .filter(line -> line.startsWith("-") && !line.startsWith("---"))
            .count();
            
        long filesChanged = diffContent.lines()
            .filter(line -> line.startsWith("diff --git"))
            .count();
        
        stats.put("lines_added", linesAdded);
        stats.put("lines_deleted", linesDeleted);
        stats.put("files_changed", filesChanged);
        stats.put("total_changes", linesAdded + linesDeleted);
        
        return stats;
    }
}