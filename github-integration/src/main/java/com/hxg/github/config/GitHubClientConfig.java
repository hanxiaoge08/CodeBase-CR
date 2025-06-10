package com.hxg.github.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * GitHub 客户端配置类
 * 
 * 负责初始化 github-api 客户端，并从配置文件中读取认证Token等信息
 */
@Configuration
public class GitHubClientConfig {

    @Value("${app.github.token:}")
    private String githubToken;

    @Value("${app.github.webhook.secret:}")
    private String webhookSecret;

    /**
     * 配置 GitHub 客户端
     * 
     * @return GitHub 客户端实例
     * @throws IOException 如果配置失败
     */
    @Bean
    public GitHub gitHubClient() throws IOException {
        if (githubToken == null || githubToken.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub token is required. Please set app.github.token in application.yml");
        }
        
        return new GitHubBuilder()
                .withOAuthToken(githubToken)
                .build();
    }

    /**
     * 获取 Webhook 密钥
     * 
     * @return Webhook 密钥
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }
} 