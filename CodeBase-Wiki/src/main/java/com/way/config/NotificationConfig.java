package com.way.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知配置类
 * 支持多种通知渠道的配置
 */
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationConfig {

    private boolean enabled = true;
    private List<String> channels = List.of("feishu", "dingtalk");
    private Feishu feishu = new Feishu();
    private Dingtalk dingtalk = new Dingtalk();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public Feishu getFeishu() {
        return feishu;
    }

    public void setFeishu(Feishu feishu) {
        this.feishu = feishu;
    }

    public Dingtalk getDingtalk() {
        return dingtalk;
    }

    public void setDingtalk(Dingtalk dingtalk) {
        this.dingtalk = dingtalk;
    }

    public static class Feishu {
        private boolean enabled = true;
        private String wikiWebhook;
        private String wikiSecret;
        private String reviewWebhook;
        private String reviewSecret;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWikiWebhook() {
            return wikiWebhook;
        }

        public void setWikiWebhook(String wikiWebhook) {
            this.wikiWebhook = wikiWebhook;
        }

        public String getWikiSecret() {
            return wikiSecret;
        }

        public void setWikiSecret(String wikiSecret) {
            this.wikiSecret = wikiSecret;
        }

        public String getReviewWebhook() {
            return reviewWebhook;
        }

        public void setReviewWebhook(String reviewWebhook) {
            this.reviewWebhook = reviewWebhook;
        }

        public String getReviewSecret() {
            return reviewSecret;
        }

        public void setReviewSecret(String reviewSecret) {
            this.reviewSecret = reviewSecret;
        }
    }

    public static class Dingtalk {
        private boolean enabled = true;
        private String wikiWebhook;
        private String wikiSecret;
        private String reviewWebhook;
        private String reviewSecret;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWikiWebhook() {
            return wikiWebhook;
        }

        public void setWikiWebhook(String wikiWebhook) {
            this.wikiWebhook = wikiWebhook;
        }

        public String getWikiSecret() {
            return wikiSecret;
        }

        public void setWikiSecret(String wikiSecret) {
            this.wikiSecret = wikiSecret;
        }

        public String getReviewWebhook() {
            return reviewWebhook;
        }

        public void setReviewWebhook(String reviewWebhook) {
            this.reviewWebhook = reviewWebhook;
        }

        public String getReviewSecret() {
            return reviewSecret;
        }

        public void setReviewSecret(String reviewSecret) {
            this.reviewSecret = reviewSecret;
        }
    }
}
