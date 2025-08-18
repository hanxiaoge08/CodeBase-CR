package com.way.crApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * 为HTTP客户端提供配置
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建RestTemplate Bean
     *
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * 配置HTTP请求工厂
     *
     * @return HTTP请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时时间（毫秒）
        factory.setConnectTimeout(10000);
        // 读取超时时间（毫秒）
        factory.setReadTimeout(30000);
        return factory;
    }
}
