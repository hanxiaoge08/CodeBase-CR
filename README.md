# CodeBase-CR - AI代码审查工具

基于Spring AI的GitHub代码审查工具，能够自动分析Pull Request并提供智能的代码审查建议。

## 功能特性

- 🤖 **AI驱动**: 基于大语言模型的智能代码审查
- 📚 **双知识库**: 结合编码规范和项目上下文的RAG检索
- 🔗 **GitHub集成**: 无缝集成GitHub Webhook和API
- ⚡ **异步处理**: 高性能的异步任务处理
- 🎯 **精确定位**: 提供文件和行级别的具体建议
- 📊 **详细报告**: 生成包含统计信息的审查报告

## 技术栈

- **Java 21** + **Spring Boot 3.x**
- **Spring AI** - AI集成框架
- **ChromaDB** - 向量数据库
- **GitHub API** - GitHub集成
- **OpenAI/阿里云通义千问** - 大语言模型

## 快速开始

### 1. 环境准备

确保已安装：
- Java 21+
- Maven 3.8+
- Docker (用于ChromaDB)

### 2. 配置环境变量

```bash
# OpenAI配置
export OPENAI_API_KEY=your-openai-api-key

# 或者使用阿里云通义千问
export DASHSCOPE_API_KEY=your-dashscope-api-key

# GitHub配置
export GITHUB_TOKEN=your-github-token
export GITHUB_WEBHOOK_SECRET=your-webhook-secret
```

### 3. 启动ChromaDB

```bash
docker run -d --name chroma -p 8000:8000 chromadb/chroma:latest
```

### 4. 运行应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动。

### 5. 配置GitHub Webhook

在GitHub仓库设置中添加Webhook：
- **Payload URL**: `http://your-domain:8080/api/v1/github/events`
- **Content type**: `application/json`
- **Secret**: 你的webhook密钥
- **Events**: 选择 "Pull requests"

## 配置说明

### application.yml 配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.1

app:
  github:
    token: ${GITHUB_TOKEN}
    webhook:
      secret: ${GITHUB_WEBHOOK_SECRET}
```

### 支持的LLM提供商

1. **OpenAI** (默认)
   - GPT-4o-mini (推荐)
   - GPT-4
   - GPT-3.5-turbo

2. **阿里云通义千问**
   - qwen-turbo
   - qwen-plus
   - qwen-max

## API端点

### 健康检查
```
GET /api/v1/github/health
```

### Webhook接收
```
POST /api/v1/github/events
```

## 工作流程

1. **接收事件**: GitHub发送Pull Request事件到Webhook端点
2. **验证签名**: 验证GitHub Webhook签名确保安全性
3. **解析事件**: 提取PR信息并创建审查任务
4. **获取代码**: 通过GitHub API获取PR的diff内容
5. **知识检索**: 使用RAG从知识库中检索相关上下文
6. **AI审查**: 调用LLM分析代码并生成审查建议
7. **发布结果**: 将审查结果发布为PR评论

## 项目结构

```
src/main/java/com/hxg/aicodereviewer/
├── config/                 # 配置类
├── controller/             # 控制器
├── dto/                    # 数据传输对象
├── service/                # 业务逻辑
│   ├── port/              # 服务接口
│   └── impl/              # 服务实现
├── adapter/                # 适配器层
│   ├── github/            # GitHub API适配器
│   └── llm/               # LLM适配器
├── knowledge/              # 知识库管理
└── exception/              # 异常定义
```

## 开发指南

### 添加新的编码规范

1. 在 `src/main/resources/knowledge/` 目录下添加规范文档
2. 重启应用，系统会自动加载新的规范到知识库

### 扩展LLM支持

1. 在 `LlmAdapter` 中添加新的LLM客户端
2. 更新配置文件添加相应的配置项
3. 实现对应的提示词模板

### 自定义审查规则

修改 `LlmAdapter.buildReviewPrompt()` 方法中的提示词模板。

## 部署

### Docker部署

```bash
# 构建镜像
docker build -t codebase-cr .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e OPENAI_API_KEY=your-key \
  -e GITHUB_TOKEN=your-token \
  --name codebase-cr \
  codebase-cr
```

### 生产环境建议

1. 使用外部向量数据库（如Milvus、Postgres+pgvector）
2. 配置负载均衡和高可用
3. 设置监控和日志收集
4. 使用GitHub App Token获得更高的API配额

## 故障排除

### 常见问题

1. **Webhook验证失败**
   - 检查webhook密钥配置
   - 确认GitHub设置中的密钥与应用配置一致

2. **LLM调用失败**
   - 检查API密钥是否正确
   - 确认网络连接正常
   - 查看API配额是否充足

3. **向量数据库连接失败**
   - 确认ChromaDB服务正在运行
   - 检查端口配置是否正确

### 日志查看

```bash
# 查看应用日志
docker logs codebase-cr

# 实时查看日志
docker logs -f codebase-cr
```

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 创建Pull Request

## 许可证

MIT License

## 联系方式

如有问题或建议，请创建Issue或联系开发团队。 