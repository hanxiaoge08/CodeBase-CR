package com.way.controller;

import com.way.service.CodeReviewESService;
import com.way.service.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author way
 */
@RestController
@RequestMapping("/api")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private CodeReviewESService codeReviewESService;

    /**
     * 健康检查接口
     * 供OpenFeign客户端检查服务状态
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 检查核心服务状态
            boolean ragServiceAvailable = ragService.isServiceAvailable();
            boolean esServiceAvailable = codeReviewESService.isServiceAvailable();
            
            health.put("status", ragServiceAvailable && esServiceAvailable ? "UP" : "DEGRADED");
            health.put("service", "CodeBase-Wiki");
            health.put("timestamp", System.currentTimeMillis());
            health.put("version", "1.0.0");
            
            // 详细状态
            Map<String, Object> details = new HashMap<>();
            details.put("ragService", ragServiceAvailable ? "UP" : "DOWN");
            details.put("esService", esServiceAvailable ? "UP" : "DOWN");
            health.put("details", details);
            
            logger.debug("健康检查完成: ragService={}, esService={}", ragServiceAvailable, esServiceAvailable);
            
        } catch (Exception e) {
            logger.error("健康检查失败: {}", e.getMessage(), e);
            health.put("status", "DOWN");
            health.put("service", "CodeBase-Wiki");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
        }
        
        return health;
    }
}
