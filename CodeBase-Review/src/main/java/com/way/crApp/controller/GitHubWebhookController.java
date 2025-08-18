package com.way.crApp.controller;

import com.way.crApp.config.GitHubClientConfig;
import com.way.crApp.service.port.IGithubWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * GitHub Webhook 控制器
 * 
 * 职责：作为系统的HTTP入口，提供 /api/v1/github/events 端点
 * 核心方法：handlePullRequestEvent()
 * 流程：接收请求 -> 使用密钥验证 X-Hub-Signature-256 签名 -> 若验证通过，调用 IGithubWebhookService 处理业务逻辑 -> 立即返回 200 OK 给GitHub，避免超时
 */
@RestController
@RequestMapping("/api/v1/github")
public class GitHubWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookController.class);
    
    @Autowired
    private IGithubWebhookService githubWebhookService;
    
    @Autowired
    private GitHubClientConfig gitHubClientConfig;

    /**
     * 处理 GitHub Pull Request 事件
     * 
     * @param signature GitHub签名头
     * @param eventType GitHub事件类型
     * @param payload 事件负载
     * @return 响应结果
     */
    @PostMapping("/events")
    public ResponseEntity<String> handleWebhookEvent(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String payload) {
        
        logger.info("接收到GitHub Webhook事件: {}", eventType);
        
        try {
            // 验证签名
            if (!verifySignature(signature, payload)) {
                logger.error("Webhook签名验证失败");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            //只处理 pull_request 事件
            if (!"pull_request".equals(eventType)) {
                logger.info("忽略非pull_request事件: {}", eventType);
                return ResponseEntity.ok("Event ignored");
            }
            
            // 异步处理事件
            githubWebhookService.handlePullRequestEvent(payload);
            
            // 立即返回200给GitHub
            return ResponseEntity.ok("Event received");
            
        } catch (Exception e) {
            logger.error("处理Webhook事件时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CodeBase-CR is running");
    }

    /**
     * 验证GitHub Webhook签名
     * 
     * @param signature GitHub签名
     * @param payload 负载内容
     * @return 验证结果
     */
    private boolean verifySignature(String signature, String payload) {
        if (signature == null || signature.isEmpty()) {
            logger.warn("缺少签名头");
            return false;
        }
        
        String webhookSecret = gitHubClientConfig.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.warn("未配置Webhook密钥，跳过签名验证");
            return true; // 如果未配置密钥，跳过验证（开发环境）
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(digest);
            
            return expectedSignature.equals(signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("签名验证时发生错误", e);
            return false;
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
} 