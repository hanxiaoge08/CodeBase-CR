# 代码评审通知系统集成说明

## 概述

本文档描述了在代码评审系统中集成的通知功能，支持飞书和钉钉的消息推送，并具有良好的可扩展性以支持其他通知渠道。

## 功能特性

### 1. 多渠道支持
- ✅ 飞书通知
- ✅ 钉钉通知
- 🔧 可扩展支持其他通知渠道（微信、邮件等）

### 2. 异步处理
- 通知发送采用异步处理，不影响主业务流程
- 专用线程池管理通知任务
- 并行发送到多个渠道，提高效率

### 3. 可配置性
- 支持全局开关控制
- 支持单个渠道的启用/禁用
- 灵活的配置管理

### 4. 消息内容丰富
- 包含仓库信息、PR详情、评审结果
- 显示主要问题摘要
- 智能化的消息格式

## 系统架构

### 核心组件

```
NotificationService (接口)
├── NotificationServiceImpl (实现)
├── NotificationChannelFactory (工厂)
└── NotificationChannel (抽象基类)
    ├── FeishuNotificationChannel (飞书实现)
    └── DingtalkNotificationChannel (钉钉实现)
```

### 配置管理

```
NotificationConfig
├── enabled (全局开关)
├── channels (启用渠道列表)
├── feishu (飞书配置)
└── dingtalk (钉钉配置)
```

## 集成点

### 1. CodeReviewServiceImpl

在以下两个方法中集成了通知功能：

- `performGraphBasedReview()` - Graph工作流审查完成后
- `performTraditionalReview()` - 传统流程审查完成后

### 2. 调用时机

```java
// 发布审查结果后立即发送通知
resultPublishService.publishReviewResult(task, result);

// 异步发送通知，不影响主流程
notificationService.sendReviewCompletionNotification(task, result);
```

## 配置说明

### application.yml 配置

```yaml
notification:
  # 是否启用通知功能
  enabled: true
  # 启用的通知渠道
  channels:
    - feishu
    - dingtalk
  
  # 飞书配置
  feishu:
    enabled: true
    # Wiki文档机器人
    wiki-webhook: your-feishu-wiki-webhook
    wiki-secret: your-feishu-wiki-secret
    # 代码评审机器人
    review-webhook: your-feishu-review-webhook
    review-secret: your-feishu-review-secret
  
  # 钉钉配置
  dingtalk:
    enabled: true
    # Wiki文档机器人
    wiki-webhook: your-dingtalk-wiki-webhook
    wiki-secret: your-dingtalk-wiki-secret
    # 代码评审机器人
    review-webhook: your-dingtalk-review-webhook
    review-secret: your-dingtalk-review-secret
```

## 扩展新的通知渠道

### 1. 创建通知渠道实现

```java
@Component
public class WeChatNotificationChannel extends NotificationChannel {
    
    public WeChatNotificationChannel(NotificationConfig config, RestTemplate restTemplate) {
        super("wechat");
        // 初始化代码
    }
    
    @Override
    public void sendReviewNotification(ReviewTaskDTO task, ReviewResultDTO result) {
        // 实现微信通知逻辑
    }
    
    @Override
    public void sendWikiNotification(String taskId, String repoName, int documentCount) {
        // 实现微信通知逻辑
    }
    
    @Override
    public boolean isEnabled() {
        // 返回是否启用
    }
}
```

### 2. 更新配置类

在 `NotificationConfig` 中添加新的配置节点：

```java
public static class WeChat {
    private boolean enabled = true;
    private String webhook;
    private String secret;
    // getter/setter
}
```

### 3. 更新配置文件

```yaml
notification:
  channels:
    - feishu
    - dingtalk
    - wechat  # 新增
  wechat:     # 新增配置节点
    enabled: true
    webhook: your-webhook-url
    secret: your-secret
```

## 消息格式示例

### 代码评审完成通知

```
🔍 代码评审完成通知

📂 仓库：user/repository
🔗 PR #123：Add new feature
👤 作者：developer
⭐ 评级：GOOD

📝 发现问题：2 个
🔧 需要修复的主要问题：
  • 变量命名不规范
  • 缺少异常处理

📊 详细报告请查看GitHub PR页面
```

## 错误处理

### 1. 优雅降级
- 通知发送失败不影响主业务流程
- 详细的错误日志记录
- 自动重试机制（可配置）

### 2. 监控和报警
- 通知发送成功/失败的日志记录
- 性能监控（发送耗时、成功率等）

## 安全考虑

### 1. 签名验证
- 飞书和钉钉都使用HMAC-SHA256签名验证
- 时间戳防重放攻击

### 2. 配置安全
- 敏感信息（webhook地址、密钥）通过配置文件管理
- 支持环境变量覆盖配置

## 性能优化

### 1. 异步处理
- 专用线程池处理通知任务
- 并行发送到多个渠道

### 2. 连接池
- HTTP客户端连接池复用
- 合理的超时配置

## 使用示例

### 发送代码评审通知

```java
// 在代码评审完成后
ReviewResultDTO result = performCodeReview(task);
resultPublishService.publishReviewResult(task, result);

// 发送通知（异步）
notificationService.sendReviewCompletionNotification(task, result);
```

### 发送Wiki完成通知

```java
// 在Wiki生成完成后
notificationService.sendWikiCompletionNotification(taskId, repoName, documentCount);
```

## 故障排查

### 1. 通知未发送
- 检查全局开关 `notification.enabled`
- 检查渠道配置 `notification.channels`
- 检查具体渠道的启用状态

### 2. 发送失败
- 查看日志中的错误信息
- 验证webhook地址和密钥
- 检查网络连接

### 3. 性能问题
- 监控线程池使用情况
- 检查HTTP连接超时配置
- 分析发送耗时日志

## 后续改进

1. **消息模板化**：支持自定义消息模板
2. **批量通知**：支持批量发送通知
3. **通知去重**：避免重复通知
4. **消息队列**：使用消息队列提高可靠性
5. **监控面板**：通知发送状态的可视化监控
