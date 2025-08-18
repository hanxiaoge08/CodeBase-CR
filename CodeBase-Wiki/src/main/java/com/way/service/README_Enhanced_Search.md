# 增强混合检索系统使用说明

## 概述
本系统实现了基于Elasticsearch的增强混合检索功能，结合BM25文本检索和kNN向量检索，使用RRF（Reciprocal Rank Fusion）算法进行结果融合，为RAG（Retrieval-Augmented Generation）系统提供高质量的上下文内容。

## 核心特性

### 1. 真正的混合检索
- **BM25检索**: 分别对代码块和文档进行精确的文本匹配
- **kNN向量检索**: 基于语义相似度的向量检索
- **RRF融合**: 使用倒数排名融合算法合并BM25和kNN结果

### 2. TopK上下文策略
- 默认TopK设置为10，确保高质量的上下文内容
- 分别获取BM25 TopK和向量TopK，然后进行融合
- 避免信息过载，提升大模型推理效果

### 3. 任务导向设计
- 所有检索都基于taskId进行过滤
- 支持任务级别的知识隔离
- 保持向后兼容性（仍支持repoId）

## 系统架构

```
用户查询 → RAGService → EnhancedHybridSearchService → Elasticsearch
                    ↓
              RAGContextBuilder → SpringAI ChatClient → 增强回答
```

### 核心组件

1. **EnhancedHybridSearchService**: 增强混合检索服务
2. **RAGService**: RAG业务逻辑服务
3. **RAGContextBuilder**: RAG上下文构建器

## API接口

### 1. RAG对话接口
```http
GET /api/chat/call?query=查询内容&taskId=任务ID&useRAG=true&topK=10
```

### 2. RAG检索接口
```http
GET /api/chat/search?query=查询内容&taskId=任务ID&topK=10
```

### 3. 纯检索接口
```http
GET /api/chat/searchOnly?query=查询内容&taskId=任务ID&topK=10
```

## RRF融合算法

系统使用RRF算法融合BM25和kNN检索结果：

```java
RRF分数 = 1 / (rank + 60)
最终分数 = BM25_RRF分数 + kNN_RRF分数
```

优势：
- 有效平衡文本匹配和语义相似度
- 避免单一检索方式的局限性
- 提升整体检索质量

## 使用示例

### Java代码示例
```java
@Autowired
private RAGService ragService;

// RAG对话
ChatResponseDTO response = ragService.processChat(
    "如何实现用户认证？", 
    "task_123", 
    true, 
    10
);

// 纯检索
SearchOnlyResponseDTO searchResult = ragService.processSearchOnly(
    "Spring Security配置", 
    "task_123", 
    10
);
```

### HTTP请求示例
```bash
# RAG对话
curl "http://localhost:8080/api/chat/call?query=如何实现用户认证&taskId=task_123&topK=10"

# 检索+AI回答
curl "http://localhost:8080/api/chat/search?query=Spring Security配置&taskId=task_123&topK=10"

# 纯检索
curl "http://localhost:8080/api/chat/searchOnly?query=数据库连接&taskId=task_123&topK=10"
```

## 性能优化

### 1. 检索参数调优
- `k`: kNN检索的候选数量，设为 topK * 2
- `numCandidates`: kNN扫描的候选数量，设为 topK * 10
- `minimumShouldMatch`: BM25查询的最小匹配度，设为75%

### 2. 上下文长度控制
- 默认最大上下文长度: 8000字符
- 代码块内容截断: 800字符
- 文档内容截断: 1000字符

### 3. 分字段权重设置
- 代码块: `content^2.0, apiName^1.8, docSummary^1.5, className^1.2, methodName^1.2`
- 文档: `content^2.0, name^1.5`

## 监控与日志

系统提供详细的日志记录：
- 检索过程跟踪
- RRF融合统计
- 性能指标监控
- 错误处理记录

## 扩展性

### 1. 支持更多检索策略
- 可扩展添加其他排序算法
- 支持自定义融合权重
- 可配置化的检索参数

### 2. 多模态检索
- 预留接口支持图片检索
- 可扩展支持代码语法检索
- 支持结构化数据检索

## 最佳实践

1. **合理设置TopK**: 根据应用场景调整TopK值，一般10-20为最佳
2. **任务隔离**: 确保不同任务使用不同的taskId
3. **定期更新向量**: 保持向量索引的时效性
4. **监控检索质量**: 定期评估检索结果的相关性

## 故障排除

### 常见问题
1. **向量生成失败**: 检查VectorEmbeddingService状态
2. **检索结果为空**: 确认taskId是否正确，索引是否存在数据
3. **性能问题**: 调整numCandidates参数，检查ES集群状态

### 降级策略
- 向量检索失败时，自动降级为纯BM25检索
- Elasticsearch不可用时，返回友好错误信息
- 上下文构建失败时，使用普通对话模式
