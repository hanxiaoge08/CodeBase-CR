# 钉钉飞书通知功能使用说明

## 概述

Framework模块现在提供了一个轻量级的通知功能Starter，遵循Spring Boot最佳实践：
- 只提供核心的通知组件
- 外部依赖由应用模块自己引入和配置
- 支持按需装配，缺少依赖时自动降级

## 1. 在应用模块中添加必要依赖

### Wiki模块和Review模块需要添加以下依赖：

```xml
<!-- Web和HTTP支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Redis支持（用于任务进度追踪和幂等性控制） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## 2. 应用配置

在`application.yml`中添加以下配置：

```yaml
# 通知功能配置
notification:
  enabled: true  # 启用通知功能
  
  # 钉钉配置
  ding-talk:
    # 文档RAG机器人
    doc-rag:
      enabled: true
      webhook: "your-dingtalk-wiki-webhook"
      secret: "your-dingtalk-wiki-secret"
    
    # 代码评审机器人
    code-review:
      enabled: true
      webhook: "your-dingtalk-review-webhook"
      secret: "your-dingtalk-review-secret"
  
  # 飞书配置
  feishu:
    # 文档RAG机器人
    doc-rag:
      enabled: true
      webhook: "your-feishu-wiki-webhook"
      secret: "your-feishu-wiki-secret"
    
    # 代码评审机器人
    code-review:
      enabled: true
      webhook: "your-feishu-review-webhook"
      secret: "your-feishu-review-secret"
  
  # Redis配置
  redis:
    task-key-prefix: "task:"
    idempotent-key-prefix: "idempotent:"
    idempotent-expire-ms: 600000  # 10分钟

# Redis连接配置
spring:
  data:
    redis:
      host: your-address
      port: 6379
      password: your-redis-password
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 3. Bean配置

Framework模块现在会自动提供默认的RestTemplate Bean，你无需手动配置：

```java
// 不需要手动配置，Framework会自动提供：
// - RestTemplate（如果应用没有自定义的话）
// - ObjectMapper（Spring Boot自动配置）
// - RedisTemplate（spring-boot-starter-data-redis自动配置）
```

如果你需要自定义RestTemplate（比如配置超时、连接池等），可以自己定义Bean，Framework会优先使用你的配置：

```java
@Configuration
public class AppConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 自定义配置
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        return restTemplate;
    }
}
```

## 4. 使用方式

### 4.1 Wiki模块使用

在生产端（发送任务时）：
```java
@Autowired
private DocumentGenerationProducer producer;

// 批量发送任务，自动设置总任务数
List<DocumentGenerationTask> tasks = buildTasks();
producer.sendTasks(taskId, tasks);
```

在消费端，Framework会自动：
1. 检查幂等性（避免重复处理）
2. 增加完成计数
3. 当所有任务完成时发送通知

### 4.2 Review模块使用

```java
@Autowired
private INotificationService notificationService;

// 代码评审完成后发送通知
notificationService.sendCodeReviewCompletionNotification(
    String.valueOf(prNumber), 
    repositoryName, 
    prTitle, 
    NotificationMessage.ProcessResult.SUCCESS, 
    processingTimeMs
);
```

## 5. 功能特性

### 5.1 渐进式配置
- **无Redis**: 只提供通知发送功能，不支持任务进度追踪
- **有Redis**: 完整功能，包括任务进度追踪和幂等性控制
- **无Web依赖**: 跳过Webhook客户端，只提供接口定义

### 5.2 容错处理
- 通知发送失败不影响主业务流程
- 缺少配置时自动跳过
- 异步发送，不阻塞主线程

### 5.3 幂等性控制
- 使用catalogueId作为幂等Key
- 支持消息重试和去重
- 自动过期清理

## 6. 故障排查

### 6.1 检查自动配置
```bash
# 启动应用时添加debug参数
java -jar app.jar --debug
```

### 6.2 检查Bean是否正确注入
查看启动日志中的条件匹配报告

### 6.3 常见问题
1. **NotificationService未注入**: 检查是否添加了Framework依赖
2. **Redis连接失败**: 检查Redis配置和网络连接
3. **Webhook发送失败**: 检查webhook地址和密钥配置

## 7. 最佳实践

1. **生产环境**: 建议启用Redis来支持完整的任务进度追踪
2. **开发环境**: 可以禁用通知功能或只启用日志输出
3. **配置管理**: 将敏感信息（如webhook密钥）放在环境变量中
4. **监控**: 建议添加通知发送成功率的监控指标
