package com.way.service.notification;

import com.way.config.NotificationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞书通知渠道实现
 */
@Component
@Slf4j
public class FeishuNotificationChannel extends NotificationChannel {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final NotificationConfig notificationConfig;
    private final RestTemplate restTemplate;

    public FeishuNotificationChannel(NotificationConfig notificationConfig,
                                   RestTemplate restTemplate) {
        super("feishu");
        this.notificationConfig = notificationConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendWikiNotification(String taskId, String repoName, int documentCount) {
        if (!isEnabled()) {
            log.debug("飞书Wiki通知已禁用，跳过发送");
            return;
        }

        try {
            String webhook = notificationConfig.getFeishu().getWikiWebhook();
            String secret = notificationConfig.getFeishu().getWikiSecret();
            String message = buildWikiCompletionMessage(taskId, repoName, documentCount);

            sendFeishuMessage(webhook, secret, message, "Wiki文档生成完成");
            log.info("飞书Wiki通知发送成功: taskId={}, repo={}", taskId, repoName);
            
        } catch (Exception e) {
            log.error("发送飞书Wiki通知失败: taskId={}, repo={}", taskId, repoName, e);
        }
    }

    @Override
    public void sendWikiFailureNotification(String taskId, String repoName, String errorMessage) {
        if (!isEnabled()) {
            log.debug("飞书Wiki失败通知已禁用，跳过发送");
            return;
        }

        try {
            String webhook = notificationConfig.getFeishu().getWikiWebhook();
            String secret = notificationConfig.getFeishu().getWikiSecret();
            String message = buildWikiFailureMessage(taskId, repoName, errorMessage);

            sendFeishuMessage(webhook, secret, message, "Wiki文档生成失败");
            log.info("飞书Wiki失败通知发送成功: taskId={}, repo={}", taskId, repoName);
            
        } catch (Exception e) {
            log.error("发送飞书Wiki失败通知失败: taskId={}, repo={}", taskId, repoName, e);
        }
    }

    @Override
    public boolean isEnabled() {
        return notificationConfig.isEnabled() && 
               notificationConfig.getFeishu().isEnabled() &&
               notificationConfig.getChannels().contains("feishu");
    }

    /**
     * 发送飞书消息
     *
     * @param webhook 飞书机器人webhook地址
     * @param secret 飞书机器人密钥
     * @param content 消息内容
     * @param title 消息标题
     */
    private void sendFeishuMessage(String webhook, String secret, String content, String title) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String sign = generateFeishuSign(secret, timestamp);

            Map<String, Object> payload = new HashMap<>();
            payload.put("timestamp", String.valueOf(timestamp));
            payload.put("sign", sign);
            payload.put("msg_type", "text");

            Map<String, String> textContent = new HashMap<>();
            textContent.put("text", content);
            payload.put("content", textContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(webhook, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("飞书消息发送成功: {}", title);
            } else {
                log.warn("飞书消息发送失败: {}, 状态码: {}", title, response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("发送飞书消息时发生异常: {}", title, e);
            throw new RuntimeException("飞书消息发送失败", e);
        }
    }

    /**
     * 生成飞书签名
     *
     * @param secret 密钥
     * @param timestamp 时间戳
     * @return 签名
     */
    private String generateFeishuSign(String secret, long timestamp) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signData = mac.doFinal(new byte[]{});
            return Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成飞书签名失败", e);
            throw new RuntimeException("飞书签名生成失败", e);
        }
    }
}
