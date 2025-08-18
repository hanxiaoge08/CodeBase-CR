package com.way.crApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Code Reviewer Application
 * 
 * 基于Spring AI的GitHub代码审查工具
 * 
 * @author CodeBase-CR Team
 * @version 1.0
 */
@SpringBootApplication
@EnableAsync
@EnableFeignClients
public class AiCodeReviewerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeReviewerApplication.class, args);
    }
} 