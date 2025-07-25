package com.alibaba.example.chatmemory.controller;

import com.alibaba.example.chatmemory.mem0.MemZeroServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查API控制器
 * 提供系统健康状态检查接口
 * 
 * @author AI Assistant
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired
    private MemZeroServiceClient memZeroServiceClient;
    
    /**
     * 基础健康检查
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Mem0 记忆系统");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 详细健康检查
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Mem0 记忆系统");
        health.put("version", "1.0.0");
        
        // 检查Mem0服务连接
        Map<String, Object> mem0Status = new HashMap<>();
        try {
            // 尝试获取记忆（使用空查询来测试连接）
            memZeroServiceClient.getAllMemories("health-check", null, null);
            mem0Status.put("status", "UP");
            mem0Status.put("message", "Mem0服务连接正常");
        } catch (Exception e) {
            mem0Status.put("status", "DOWN");
            mem0Status.put("message", "Mem0服务连接失败: " + e.getMessage());
            logger.warn("Mem0健康检查失败", e);
        }
        
        health.put("mem0", mem0Status);
        
        // 系统整体状态
        boolean isHealthy = "UP".equals(mem0Status.get("status"));
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 检查Mem0服务状态
     */
    @GetMapping("/mem0")
    public ResponseEntity<Map<String, Object>> checkMem0() {
        
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "Mem0");
        
        try {
            // 测试Mem0连接
            memZeroServiceClient.getAllMemories("ping", null, null);
            
            status.put("status", "UP");
            status.put("message", "Mem0服务可用");
            status.put("responseTime", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("message", "Mem0服务不可用: " + e.getMessage());
            
            logger.error("Mem0服务健康检查失败", e);
            
            return ResponseEntity.status(503).body(status);
        }
    }
    
    /**
     * 就绪状态检查
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        
        Map<String, Object> readiness = new HashMap<>();
        readiness.put("timestamp", LocalDateTime.now());
        
        // 检查必要的服务是否就绪
        boolean mem0Ready = false;
        try {
            memZeroServiceClient.getAllMemories("readiness-check", null, null);
            mem0Ready = true;
        } catch (Exception e) {
            logger.debug("Mem0就绪检查失败", e);
        }
        
        boolean isReady = mem0Ready;
        
        readiness.put("status", isReady ? "READY" : "NOT_READY");
        readiness.put("mem0", mem0Ready ? "READY" : "NOT_READY");
        
        if (isReady) {
            return ResponseEntity.ok(readiness);
        } else {
            return ResponseEntity.status(503).body(readiness);
        }
    }
    
    /**
     * 存活状态检查
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> live() {
        
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", LocalDateTime.now());
        liveness.put("uptime", System.currentTimeMillis());
        
        return ResponseEntity.ok(liveness);
    }
} 