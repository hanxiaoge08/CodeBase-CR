# Wiki模块通知系统集成说明

## 概述

本文档描述了在Wiki文档生成模块中集成的通知功能，实现了基于Redis的任务进度跟踪、幂等性控制和多渠道通知推送。

## 功能特性

### 1. 任务进度跟踪
- 基于Redis实现任务总数和完成数的跟踪
- 原子操作确保计数准确性
- 支持任务完成状态检查

### 2. 幂等性控制
- 基于catalogueId实现消息幂等性
- 使用Lua脚本确保原子性操作
- 支持处理中、已完成状态判断

### 3. 多渠道通知
- 支持飞书和钉钉通知
- 异步并行发送，提高效率
- 可扩展支持其他通知渠道

### 4. 故障处理
- 失败任务的通知推送
- 重试机制和死信队列处理
- 优雅的错误处理和降级

## 系统架构

### 核心组件

```
ITaskProgressService (Redis服务接口)
├── TaskProgressServiceImpl (Redis实现)
├── INotificationService (通知服务接口)
├── NotificationServiceImpl (通知服务实现)
└── NotificationChannel (通知渠道抽象)
    ├── FeishuNotificationChannel (飞书通知)
    └── DingtalkNotificationChannel (钉钉通知)
```

### Redis数据结构

```
task:{taskId}:total      // Long，总任务数（生产端设置）
task:{taskId}:consume    // Long，已消费成功的任务数
idempotent:catalogue:{catalogueId}  // 幂等性控制（过期时间10分钟）
```

## 集成实现

### 1. processTask方法完善

在`DocumentGenerationConsumer.processTask()`方法中集成了以下功能：

#### 1.1 幂等性检查
```java
// 1. 幂等性检查
ITaskProgressService.IdempotentResult idempotentResult = 
    taskProgressService.checkIdempotent(task.getCatalogueId());
    
switch (idempotentResult) {
    case COMPLETED:
        // 消息已处理完成，跳过处理
        ack.acknowledge();
        return;
    case PROCESSING:
        // 消息正在处理中，抛异常重试
        throw new RuntimeException("消息正在处理中，需要重试");
    case FIRST_TIME:
        // 第一次处理，继续执行
        break;
}
```

#### 1.2 任务进度跟踪
```java
// 增加完成任务数
long completedCount = taskProgressService.incrementConsumedCount(task.getTaskId());

// 检查是否所有任务都已完成
if (taskProgressService.isTaskCompleted(task.getTaskId())) {
    // 发送完成通知
    notificationService.sendWikiCompletionNotification(
        task.getTaskId(), 
        repoName, 
        (int) totalCount
    );
    
    // 清理Redis数据
    taskProgressService.clearTaskProgress(task.getTaskId());
}
```

#### 1.3 幂等性标记
```java
// 处理成功后标记幂等性完成
taskProgressService.markMessageCompleted(task.getCatalogueId());
```

### 2. 失败通知处理

```java
// 发送失败通知（如果超过最大重试次数）
if (task.exceedsMaxRetries(maxRetry)) {
    String repoName = getRepoNameFromTask(task);
    notificationService.sendWikiFailureNotification(
        task.getTaskId(), 
        repoName, 
        e.getMessage()
    );
}
```

## Redis幂等性实现

### Lua脚本
```lua
local key = KEYS[1]
local value = ARGV[1]
local expire_time_ms = ARGV[2]
return redis.call('SET', key, value, 'NX', 'GET', 'PX', expire_time_ms)
```

### 处理逻辑
- **返回null**: 第一次处理，设置状态为0（处理中）
- **返回"0"**: 正在处理中，抛异常重试
- **返回"1"**: 已处理完成，跳过处理

## 配置说明

### application.yml配置

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
    wiki-webhook: your-feishu-wiki-webhook
    wiki-secret: your-feishu-wiki-secret
  
  # 钉钉配置
  dingtalk:
    enabled: true
    wiki-webhook: your-dingtalk-wiki-webhook
    wiki-secret: your-dingtalk-wiki-secret

# Redis配置
spring:
  data:
    redis:
      host: your-address
      port: 6379
      password: your-redis-password
      timeout: 2000ms
```

## 依赖配置

### pom.xml添加依赖

```xml
<!-- Spring Boot Redis Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## 使用流程

### 1. 任务生产端设置总数
```java
// 在发送kafka消息前设置总任务数
taskProgressService.setTaskTotal(taskId, totalCount);
```

### 2. 消费端处理流程
1. **幂等性检查**: 防止重复处理
2. **业务处理**: 执行文档生成逻辑
3. **计数更新**: 原子递增完成数
4. **完成检查**: 判断是否所有任务完成
5. **通知发送**: 发送完成或失败通知
6. **数据清理**: 清理Redis进度数据

## 消息格式示例

### Wiki完成通知
```
📚 Wiki文档生成完成通知

📂 仓库：spring-boot-demo
🆔 任务ID：task_20250115_001
📄 生成文档：25 个
✅ 状态：已完成
⏰ 完成时间：2025-01-15 14:30:25
```

### Wiki失败通知
```
❌ Wiki文档生成失败通知

📂 仓库：spring-boot-demo
🆔 任务ID：task_20250115_001
❌ 状态：处理失败
🔍 错误信息：连接超时
⏰ 失败时间：2025-01-15 14:30:25
```

## 监控和调试

### 1. 日志监控
- 任务处理开始/完成日志
- 幂等性检查日志
- 通知发送成功/失败日志
- Redis操作日志

### 2. Redis键监控
```bash
# 查看任务进度
redis-cli get "task:task_20250115_001:total"
redis-cli get "task:task_20250115_001:consume"

# 查看幂等性状态
redis-cli get "idempotent:catalogue:catalogue_123"
```

### 3. 调试命令
```bash
# 清理特定任务进度
redis-cli del "task:task_20250115_001:total"
redis-cli del "task:task_20250115_001:consume"

# 重置幂等性状态
redis-cli del "idempotent:catalogue:catalogue_123"
```

## 故障排查

### 1. 通知未发送
- 检查`notification.enabled`配置
- 验证渠道配置和凭据
- 查看异步线程池状态

### 2. 任务重复处理
- 检查Redis连接
- 验证Lua脚本执行
- 查看幂等性键过期时间

### 3. 计数不准确
- 检查Redis原子操作
- 验证异常处理逻辑
- 查看消息确认机制

## 扩展功能

### 1. 添加新通知渠道
1. 实现`NotificationChannel`抽象类
2. 在`NotificationConfig`中添加配置
3. 更新`application.yml`配置

### 2. 自定义消息模板
1. 重写`buildWikiCompletionMessage`方法
2. 支持模板变量替换
3. 添加国际化支持

### 3. 监控面板
1. 添加JMX监控指标
2. 集成Prometheus监控
3. 创建Grafana仪表板

## 性能优化

### 1. Redis优化
- 使用连接池
- 设置合理的过期时间
- 监控内存使用

### 2. 通知优化
- 异步并行发送
- 连接复用
- 失败重试机制

### 3. 监控优化
- 定期清理过期数据
- 监控Redis性能
- 优化Lua脚本执行
