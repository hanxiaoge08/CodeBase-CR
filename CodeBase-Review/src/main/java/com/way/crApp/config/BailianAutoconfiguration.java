package com.way.crApp.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author way
 * @description: 百炼知识库配置类
 * @date 2025/1/16 09:34
 */
@Configuration
public class BailianAutoconfiguration {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 百炼调用时需要配置 DashScope API，对 dashScopeApi 强依赖。
     * @return
     */
    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder().apiKey(apiKey).build();
    }

}