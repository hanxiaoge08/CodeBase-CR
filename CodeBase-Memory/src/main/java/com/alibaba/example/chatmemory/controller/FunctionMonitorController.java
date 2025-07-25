package com.alibaba.example.chatmemory.controller;

import com.alibaba.example.chatmemory.service.FunctionCallMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Function监控API控制器
 * 提供Function调用统计信息的查询接口
 * 
 * @author AI Assistant
 */
@RestController
@RequestMapping("/api/function-monitor")
public class FunctionMonitorController {
    
    private static final Logger logger = LoggerFactory.getLogger(FunctionMonitorController.class);
    
    @Autowired
    private FunctionCallMonitorService monitorService;
    
    /**
     * 获取Function调用统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<FunctionCallMonitorService.FunctionCallStatistics> getStatistics() {
        
        try {
            logger.info("获取Function调用统计信息");
            
            FunctionCallMonitorService.FunctionCallStatistics statistics = monitorService.getStatistics();
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("获取Function调用统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 重置统计信息
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetStatistics() {
        
        try {
            logger.info("重置Function调用统计信息");
            
            monitorService.resetStatistics();
            
            return ResponseEntity.ok("统计信息已重置");
            
        } catch (Exception e) {
            logger.error("重置Function调用统计信息失败", e);
            return ResponseEntity.internalServerError().body("重置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取监控状态
     */
    @GetMapping("/status")
    public ResponseEntity<MonitorStatus> getMonitorStatus() {
        
        try {
            FunctionCallMonitorService.FunctionCallStatistics stats = monitorService.getStatistics();
            
            MonitorStatus status = new MonitorStatus();
            status.setActive(true);
            status.setTotalCalls(stats.getTotalCallCount());
            status.setFunctionCount(stats.getFunctionStats() != null ? stats.getFunctionStats().size() : 0);
            
            // 计算总成功率
            if (stats.getFunctionStats() != null && !stats.getFunctionStats().isEmpty()) {
                double totalSuccessRate = stats.getFunctionStats().values().stream()
                    .mapToDouble(FunctionCallMonitorService.FunctionStats::getSuccessRate)
                    .average().orElse(0.0);
                status.setAverageSuccessRate(totalSuccessRate);
            } else {
                status.setAverageSuccessRate(100.0);
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("获取监控状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 监控状态
     */
    public static class MonitorStatus {
        private boolean active;
        private long totalCalls;
        private int functionCount;
        private double averageSuccessRate;
        
        // Getters and Setters
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public long getTotalCalls() { return totalCalls; }
        public void setTotalCalls(long totalCalls) { this.totalCalls = totalCalls; }
        
        public int getFunctionCount() { return functionCount; }
        public void setFunctionCount(int functionCount) { this.functionCount = functionCount; }
        
        public double getAverageSuccessRate() { return averageSuccessRate; }
        public void setAverageSuccessRate(double averageSuccessRate) { this.averageSuccessRate = averageSuccessRate; }
    }
} 