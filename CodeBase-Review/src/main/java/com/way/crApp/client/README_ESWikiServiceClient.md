# ESWikiServiceClient 使用说明

## 概述

ESWikiServiceClient 是用于Review模块调用Wiki模块ES检索服务的Feign客户端，替代了原有的MemoryServiceClient，提供更强的混合检索能力(BM25 + kNN + RRF)。

## 核心特性

1. **增强混合检索**: 基于Elasticsearch的BM25文本检索 + kNN向量检索 + RRF融合算法
2. **智能上下文构建**: 自动提取PR标题、描述、变更文件等关键信息构建检索查询
3. **降级策略**: 服务不可用时提供基本的上下文信息，保证代码评审流程不中断
4. **健康检查**: 支持服务状态监控

## 配置

### application.yml配置
```yaml
# Wiki服务配置
wiki:
  service:
    url: http://localhost:8085

# Feign配置
feign:
  client:
    config:
      wiki-service:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: basic
  hystrix:
    enabled: true
  circuitbreaker:
    enabled: true
```

## 使用方法

### 1. 基本使用
```java
@Autowired
private ESWikiServiceClient esWikiServiceClient;

public String getCodeReviewContext(String taskId, String prTitle, 
                                  String prDescription, List<String> changedFiles) {
    CodeReviewContextRequest request = new CodeReviewContextRequest();
    request.setTaskId(taskId);
    request.setPrTitle(prTitle);
    request.setPrDescription(prDescription);
    request.setChangedFiles(changedFiles);
    request.setMaxResults(10);
    
    return esWikiServiceClient.searchContextForCodeReview(request);
}
```

### 2. 在Agent中使用
```java
@Component
public class StyleConventionAgent {
    
    @Autowired
    private ESWikiServiceClient esWikiServiceClient;
    
    public void reviewStyleConventions(PullRequestEventDTO prEvent) {
        // 构建上下文请求
        CodeReviewContextRequest contextRequest = new CodeReviewContextRequest();
        contextRequest.setTaskId(prEvent.getRepository().getName());
        contextRequest.setPrTitle(prEvent.getPullRequest().getTitle());
        contextRequest.setPrDescription(prEvent.getPullRequest().getBody());
        contextRequest.setChangedFiles(extractChangedFiles(prEvent));
        
        // 获取相关上下文
        String context = esWikiServiceClient.searchContextForCodeReview(contextRequest);
        
        // 基于上下文进行编码规范评审
        if (StringUtils.hasText(context)) {
            // 使用检索到的上下文进行智能评审
            performIntelligentReview(context, prEvent);
        } else {
            // 使用基础规则进行评审
            performBasicReview(prEvent);
        }
    }
}
```

### 3. 健康检查
```java
@Autowired
private ESWikiServiceClient esWikiServiceClient;

public boolean isWikiServiceHealthy() {
    try {
        Map<String, Object> health = esWikiServiceClient.healthCheck();
        return "UP".equals(health.get("status"));
    } catch (Exception e) {
        logger.warn("Wiki服务健康检查失败: {}", e.getMessage());
        return false;
    }
}
```

## 降级处理

当Wiki服务不可用时，ESWikiServiceClientFallback会提供降级响应：

### 降级响应示例
```markdown
## 代码评审上下文 (降级模式)

⚠️ Wiki检索服务暂时不可用，以下为基本信息：

**PR标题**: 添加用户认证功能
**PR描述**: 实现JWT Token认证和权限控制
**变更文件**: 
- src/main/java/com/example/auth/AuthController.java
- src/main/java/com/example/config/SecurityConfig.java

**注意**: 由于检索服务不可用，缺少相关代码上下文，建议基于变更内容和通用最佳实践进行评审。
```

## API接口

### 1. 搜索代码评审上下文
- **路径**: `POST /api/chat/reviewContext`
- **参数**: CodeReviewContextRequest
- **返回**: 格式化的上下文字符串

### 2. 健康检查
- **路径**: `GET /api/health`
- **返回**: 服务状态信息

## CodeReviewContextRequest 参数说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| repositoryId | String | 否 | 仓库ID（兼容性字段） |
| taskId | String | 是 | 任务ID，用于过滤检索范围 |
| diffContent | String | 否 | PR的diff内容 |
| prTitle | String | 否 | PR标题 |
| prDescription | String | 否 | PR描述 |
| changedFiles | List<String> | 否 | 变更文件列表 |
| maxResults | Integer | 否 | 最大结果数，默认10 |

## 返回的上下文格式

```markdown
## 代码评审相关上下文

**PR标题**: 添加用户认证功能
**变更文件**: AuthController.java, SecurityConfig.java

### 相关代码片段

#### 代码片段 1 (相关度: 0.95)
**方法**: authenticateUser
**类**: UserAuthService
**说明**: 用户认证核心逻辑

```java
public AuthResult authenticateUser(String username, String password) {
    // 认证逻辑实现
}
```

### 相关文档

#### 文档 1 (相关度: 0.87)
**标题**: 用户认证设计文档

本文档描述了系统的用户认证架构...
```

## 最佳实践

1. **合理设置taskId**: 确保taskId与实际的代码库或项目对应
2. **提供完整信息**: 尽可能提供PR标题、描述和变更文件信息
3. **处理降级情况**: 在代码中检查返回的上下文是否为空或降级响应
4. **监控服务状态**: 定期检查Wiki服务的健康状态
5. **缓存策略**: 对于相同的请求可以考虑短期缓存，减少服务调用

## 故障排除

### 常见问题

1. **服务连接超时**
   - 检查Wiki服务是否正常运行
   - 确认网络连接和端口配置

2. **返回空上下文**
   - 确认taskId是否正确
   - 检查ES索引中是否有相关数据

3. **降级响应触发**
   - 查看Wiki服务日志
   - 检查Elasticsearch服务状态

### 监控指标

- 服务调用成功率
- 响应时间
- 降级触发频率
- 返回上下文的平均长度
