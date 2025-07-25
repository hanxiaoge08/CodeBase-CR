package com.alibaba.example.chatmemory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * Function调用监控服务
 * 用于统计和监控AI Function调用的使用情况
 * 
 * @author AI Assistant
 */
@Service
public class FunctionCallMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(FunctionCallMonitorService.class);
    
    // 总调用次数统计
    private final AtomicLong totalCallCount = new AtomicLong(0);
    
    // 各函数调用次数统计
    private final Map<String, AtomicInteger> functionCallCounts = new ConcurrentHashMap<>();
    
    // 各函数响应时间统计
    private final Map<String, AtomicLong> functionResponseTimes = new ConcurrentHashMap<>();
    
    // 错误次数统计
    private final Map<String, AtomicInteger> functionErrorCounts = new ConcurrentHashMap<>();
    
    // 最近调用记录（最多保存100条）
    private final Map<String, FunctionCallRecord> recentCalls = new ConcurrentHashMap<>();
    
    /**
     * 记录函数调用开始
     * 
     * @param functionName 函数名称
     * @param repositoryId 仓库ID
     * @param parameters 调用参数（简化版）
     * @return 调用ID，用于后续记录结束时间
     */
    public String recordFunctionCallStart(String functionName, String repositoryId, String parameters) {
        
        String callId = generateCallId(functionName, repositoryId);
        
        // 增加总调用次数
        totalCallCount.incrementAndGet();
        
        // 增加函数调用次数
        functionCallCounts.computeIfAbsent(functionName, k -> new AtomicInteger(0)).incrementAndGet();
        
        // 记录调用开始时间
        FunctionCallRecord record = new FunctionCallRecord();
        record.setFunctionName(functionName);
        record.setRepositoryId(repositoryId);
        record.setParameters(parameters);
        record.setStartTime(LocalDateTime.now());
        record.setCallId(callId);
        
        recentCalls.put(callId, record);
        
        logger.debug("记录函数调用开始: callId={}, functionName={}, repositoryId={}", 
            callId, functionName, repositoryId);
        
        return callId;
    }
    
    /**
     * 记录函数调用结束
     * 
     * @param callId 调用ID
     * @param success 是否成功
     * @param responseSize 响应大小（字符数）
     */
    public void recordFunctionCallEnd(String callId, boolean success, int responseSize) {
        
        FunctionCallRecord record = recentCalls.get(callId);
        if (record == null) {
            logger.warn("未找到调用记录: callId={}", callId);
            return;
        }
        
        // 记录结束时间和响应信息
        record.setEndTime(LocalDateTime.now());
        record.setSuccess(success);
        record.setResponseSize(responseSize);
        
        // 计算响应时间（毫秒）
        long responseTime = java.time.Duration.between(record.getStartTime(), record.getEndTime()).toMillis();
        record.setResponseTimeMs(responseTime);
        
        // 更新统计信息
        String functionName = record.getFunctionName();
        functionResponseTimes.computeIfAbsent(functionName, k -> new AtomicLong(0)).addAndGet(responseTime);
        
        // 记录错误
        if (!success) {
            functionErrorCounts.computeIfAbsent(functionName, k -> new AtomicInteger(0)).incrementAndGet();
        }
        
        logger.info("函数调用完成: callId={}, functionName={}, success={}, responseTime={}ms, responseSize={}", 
            callId, functionName, success, responseTime, responseSize);
        
        // 清理旧记录（保持最近100条）
        if (recentCalls.size() > 100) {
            cleanupOldRecords();
        }
    }
    
    /**
     * 获取函数调用统计信息
     */
    public FunctionCallStatistics getStatistics() {
        
        FunctionCallStatistics stats = new FunctionCallStatistics();
        stats.setTotalCallCount(totalCallCount.get());
        
        // 计算各函数的统计信息
        Map<String, FunctionStats> functionStats = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, AtomicInteger> entry : functionCallCounts.entrySet()) {
            String functionName = entry.getKey();
            int callCount = entry.getValue().get();
            
            FunctionStats funcStats = new FunctionStats();
            funcStats.setFunctionName(functionName);
            funcStats.setCallCount(callCount);
            
            // 计算平均响应时间
            AtomicLong totalResponseTime = functionResponseTimes.get(functionName);
            if (totalResponseTime != null && callCount > 0) {
                funcStats.setAverageResponseTimeMs(totalResponseTime.get() / callCount);
            }
            
            // 获取错误次数
            AtomicInteger errorCount = functionErrorCounts.get(functionName);
            if (errorCount != null) {
                funcStats.setErrorCount(errorCount.get());
                funcStats.setSuccessRate((double)(callCount - errorCount.get()) / callCount * 100);
            } else {
                funcStats.setSuccessRate(100.0);
            }
            
            functionStats.put(functionName, funcStats);
        }
        
        stats.setFunctionStats(functionStats);
        
        logger.debug("生成函数调用统计信息: totalCalls={}, functions={}", 
            stats.getTotalCallCount(), functionStats.size());
        
        return stats;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        logger.info("重置函数调用统计信息");
        
        totalCallCount.set(0);
        functionCallCounts.clear();
        functionResponseTimes.clear();
        functionErrorCounts.clear();
        recentCalls.clear();
    }
    
    /**
     * 生成调用ID
     */
    private String generateCallId(String functionName, String repositoryId) {
        return String.format("%s_%s_%d", 
            functionName, 
            repositoryId != null ? repositoryId : "unknown", 
            System.currentTimeMillis());
    }
    
    /**
     * 清理旧的调用记录
     */
    private void cleanupOldRecords() {
        // 简单清理：移除最旧的20条记录
        if (recentCalls.size() > 100) {
            recentCalls.entrySet().stream()
                .sorted(Map.Entry.<String, FunctionCallRecord>comparingByValue(
                    (r1, r2) -> r1.getStartTime().compareTo(r2.getStartTime())))
                .limit(20)
                .map(Map.Entry::getKey)
                .forEach(recentCalls::remove);
        }
    }
    
    /**
     * 函数调用记录
     */
    public static class FunctionCallRecord {
        private String callId;
        private String functionName;
        private String repositoryId;
        private String parameters;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean success;
        private long responseTimeMs;
        private int responseSize;
        
        // Getters and Setters
        public String getCallId() { return callId; }
        public void setCallId(String callId) { this.callId = callId; }
        
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        
        public String getRepositoryId() { return repositoryId; }
        public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; }
        
        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
        
        public int getResponseSize() { return responseSize; }
        public void setResponseSize(int responseSize) { this.responseSize = responseSize; }
    }
    
    /**
     * 函数调用统计信息
     */
    public static class FunctionCallStatistics {
        private long totalCallCount;
        private Map<String, FunctionStats> functionStats;
        
        // Getters and Setters
        public long getTotalCallCount() { return totalCallCount; }
        public void setTotalCallCount(long totalCallCount) { this.totalCallCount = totalCallCount; }
        
        public Map<String, FunctionStats> getFunctionStats() { return functionStats; }
        public void setFunctionStats(Map<String, FunctionStats> functionStats) { this.functionStats = functionStats; }
    }
    
    /**
     * 单个函数统计信息
     */
    public static class FunctionStats {
        private String functionName;
        private int callCount;
        private int errorCount;
        private long averageResponseTimeMs;
        private double successRate;
        
        // Getters and Setters
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        
        public int getCallCount() { return callCount; }
        public void setCallCount(int callCount) { this.callCount = callCount; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public long getAverageResponseTimeMs() { return averageResponseTimeMs; }
        public void setAverageResponseTimeMs(long averageResponseTimeMs) { this.averageResponseTimeMs = averageResponseTimeMs; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
} 