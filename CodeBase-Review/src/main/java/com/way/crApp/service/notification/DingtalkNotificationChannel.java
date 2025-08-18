package com.way.crApp.service.notification;


import com.way.crApp.config.NotificationConfig;
import com.way.crApp.dto.review.ReviewResultDTO;
import com.way.crApp.dto.review.ReviewTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉通知渠道实现
 */
@Component
public class DingtalkNotificationChannel extends NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(DingtalkNotificationChannel.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final NotificationConfig notificationConfig;
    private final RestTemplate restTemplate;

    public DingtalkNotificationChannel(NotificationConfig notificationConfig,
                                     RestTemplate restTemplate) {
        super("dingtalk");
        this.notificationConfig = notificationConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendReviewNotification(ReviewTaskDTO task, ReviewResultDTO result) {
        if (!isEnabled()) {
            logger.debug("钉钉代码评审通知已禁用，跳过发送");
            return;
        }

        try {
            String webhook = notificationConfig.getDingtalk().getReviewWebhook();
            String secret = notificationConfig.getDingtalk().getReviewSecret();
            String message = buildReviewMessage(task, result);

            sendDingtalkMessage(webhook, secret, message, "代码评审完成");
            logger.info("钉钉代码评审通知发送成功: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber());
                
        } catch (Exception e) {
            logger.error("发送钉钉代码评审通知失败: repo={}, pr={}", 
                task.repositoryFullName(), task.prNumber(), e);
        }
    }

    @Override
    public void sendWikiNotification(String taskId, String repoName, int documentCount) {
        if (!isEnabled()) {
            logger.debug("钉钉Wiki通知已禁用，跳过发送");
            return;
        }

        try {
            String webhook = notificationConfig.getDingtalk().getWikiWebhook();
            String secret = notificationConfig.getDingtalk().getWikiSecret();
            String message = buildWikiMessage(taskId, repoName, documentCount);

            sendDingtalkMessage(webhook, secret, message, "Wiki文档生成完成");
            logger.info("钉钉Wiki通知发送成功: taskId={}, repo={}", taskId, repoName);
            
        } catch (Exception e) {
            logger.error("发送钉钉Wiki通知失败: taskId={}, repo={}", taskId, repoName, e);
        }
    }

    @Override
    public boolean isEnabled() {
        return notificationConfig.isEnabled() && 
               notificationConfig.getDingtalk().isEnabled() &&
               notificationConfig.getChannels().contains("dingtalk");
    }

    /**
     * 发送钉钉消息
     *
     * @param webhook 钉钉机器人webhook地址
     * @param secret 钉钉机器人密钥
     * @param content 消息内容
     * @param title 消息标题
     */
    private void sendDingtalkMessage(String webhook, String secret, String content, String title) {
        try {
            long timestamp = System.currentTimeMillis();
            String sign = generateDingtalkSign(secret, timestamp);
            
            // 在webhook URL后面添加签名参数
            String finalWebhook = webhook + "&timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "text");

            Map<String, String> textContent = new HashMap<>();
            textContent.put("content", content);
            payload.put("text", textContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(finalWebhook, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("钉钉消息发送成功: {}", title);
            } else {
                logger.warn("钉钉消息发送失败: {}, 状态码: {}", title, response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("发送钉钉消息时发生异常: {}", title, e);
            throw new RuntimeException("钉钉消息发送失败", e);
        }
    }

    /**
     * 生成钉钉签名
     *
     * @param secret 密钥
     * @param timestamp 时间戳
     * @return 签名
     */
    private String generateDingtalkSign(String secret, long timestamp) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("生成钉钉签名失败", e);
            throw new RuntimeException("钉钉签名生成失败", e);
        }
    }

    @Override
    protected String buildReviewMessage(ReviewTaskDTO task, ReviewResultDTO result) {
        StringBuilder message = new StringBuilder();
        message.append("🔍 代码评审完成通知\n\n");
        message.append("📂 仓库：").append(task.repositoryFullName()).append("\n");
        message.append("🔗 PR #").append(task.prNumber()).append("：").append(task.prTitle()).append("\n");
        message.append("👤 作者：").append(task.prAuthor()).append("\n");
        message.append("⭐ 评级：").append(result.overallRating()).append("\n");

        if (result.comments() != null && !result.comments().isEmpty()) {
            message.append("📝 发现问题：").append(result.comments().size()).append(" 个\n");
            message.append("🔧 需要修复的主要问题：\n");
            
            // 显示前3个问题
            int count = Math.min(3, result.comments().size());
            for (int i = 0; i < count; i++) {
                var comment = result.comments().get(i);
                message.append("  • ").append(comment.comment()).append("\n");
            }
            
            if (result.comments().size() > 3) {
                message.append("  ... 等其他 ").append(result.comments().size() - 3).append(" 个问题\n");
            }
        } else {
            message.append("✅ 未发现问题\n");
        }

        message.append("\n📊 详细报告请查看GitHub PR页面");

        return message.toString();
    }
}
