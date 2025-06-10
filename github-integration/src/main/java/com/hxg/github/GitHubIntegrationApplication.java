package com.hxg.github;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GitHub 集成模块主应用程序
 * 
 * 提供独立的 GitHub 集成功能
 */
@SpringBootApplication
@EnableAsync
public class GitHubIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubIntegrationApplication.class, args);
    }
} 