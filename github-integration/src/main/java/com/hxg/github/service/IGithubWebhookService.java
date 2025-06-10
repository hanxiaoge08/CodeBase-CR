package com.hxg.github.service;

/**
 * GitHub Webhook 服务接口
 * 
 * 定义处理GitHub事件的业务逻辑
 */
public interface IGithubWebhookService {

    /**
     * 处理 Pull Request 事件
     * 
     * @param payload GitHub事件负载JSON字符串
     */
    void handlePullRequestEvent(String payload);
} 