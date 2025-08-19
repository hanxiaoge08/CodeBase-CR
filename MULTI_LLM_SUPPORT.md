# 多大模型支持说明

本项目现已支持多种大语言模型，可以通过配置文件轻松切换模型提供商。

## 支持的模型提供商

### 1. 阿里云 DashScope（默认）
- 提供商标识：`dashscope`
- 默认聊天模型：`qwen3-coder-plus-2025-07-22`
- 默认嵌入模型：`text-embedding-v4`
- 特点：云端服务，性能稳定，专业优化

### 2. 本地 Ollama
- 提供商标识：`ollama`
- 推荐聊天模型：`qwen2.5-coder:14b`
- 推荐嵌入模型：`nomic-embed-text`
- 特点：本地部署，数据私密，免费使用

### 3. OpenAI
- 提供商标识：`openai`
- 推荐聊天模型：`gpt-4o`
- 推荐嵌入模型：`text-embedding-3-small`
- 特点：业界领先，功能强大，API稳定

## 配置方法

### 方法一：环境变量配置

在 `.env` 文件中设置：

```bash
# 使用阿里云DashScope（默认）
SPRING_AI_PROVIDER=dashscope
DASHSCOPE_API_KEY=sk-your-api-key-here

# 使用本地Ollama
SPRING_AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=qwen2.5-coder:14b
OLLAMA_EMBEDDING_MODEL=nomic-embed-text

# 使用OpenAI
SPRING_AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-openai-api-key
OPENAI_CHAT_MODEL=gpt-4o
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

### 方法二：application.yml配置

在 `application.yml` 中设置：

```yaml
spring:
  ai:
    provider: ollama  # 或 dashscope、openai
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5-coder:14b
      embedding:
        options:
          model: nomic-embed-text
    openai:
      api-key: sk-your-openai-api-key
      chat:
        options:
          model: gpt-4o
      embedding:
        options:
          model: text-embedding-3-small
```

### 方法三：启动参数

通过 JVM 启动参数：

```bash
# 使用Ollama
java -Dspring.ai.provider=ollama -jar your-app.jar

# 使用OpenAI
java -Dspring.ai.provider=openai -Dopenai.api-key=your-key -jar your-app.jar
```

## OpenAI 配置指南

### 1. 获取 OpenAI API Key

**官方渠道:**
```bash
# 访问 OpenAI 官网获取 API Key
https://platform.openai.com/api-keys

# 创建新的API Key
# 注意：创建后请立即保存，无法再次查看
```

### 2. 配置 API Key

```bash
# 在.env文件中设置
SPRING_AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-openai-api-key

# 可选：使用代理服务
OPENAI_BASE_URL=https://your-proxy-service.com/v1
```

### 3. 推荐模型选择

```bash
# 代码任务优选
OPENAI_CHAT_MODEL=gpt-4o              # 最新最强
OPENAI_CHAT_MODEL=gpt-4o-mini         # 性价比高
OPENAI_CHAT_MODEL=o1-preview          # 推理能力强

# 嵌入模型
OPENAI_EMBEDDING_MODEL=text-embedding-3-small   # 性价比好
OPENAI_EMBEDDING_MODEL=text-embedding-3-large   # 效果更好
```

### 4. 成本控制建议

```bash
# 开发环境：使用经济模型
OPENAI_CHAT_MODEL=gpt-4o-mini

# 生产环境：平衡性能和成本
OPENAI_CHAT_MODEL=gpt-4o

# 预算充足：使用最强模型
OPENAI_CHAT_MODEL=o1-preview
```

## Ollama 本地部署指南

### 1. 安装 Ollama

**Windows/Mac/Linux:**
```bash
# 访问 https://ollama.ai 下载安装包
# 或使用包管理器安装
curl -fsSL https://ollama.ai/install.sh | sh
```

### 2. 下载推荐模型

```bash
# 下载代码专用模型（推荐）
ollama pull qwen2.5-coder:14b

# 下载嵌入模型
ollama pull nomic-embed-text

# 其他可选模型
ollama pull qwen2.5:14b          # 通用任务
ollama pull codellama:13b        # 代码任务
ollama pull llama3.1:8b          # 轻量级模型
ollama pull mxbai-embed-large    # 更好的嵌入模型
```

### 3. 启动 Ollama 服务

```bash
# 启动服务（默认端口11434）
ollama serve

# 验证服务
curl http://localhost:11434/api/tags
```

### 4. 测试模型

```bash
# 测试聊天模型
ollama run qwen2.5-coder:14b "写一个Java Hello World程序"

# 查看已下载的模型
ollama list
```

## 模型切换步骤

### 从阿里云切换到本地Ollama

1. **安装并启动Ollama服务**
   ```bash
   ollama serve
   ```

2. **下载模型**
   ```bash
   ollama pull qwen2.5-coder:14b
   ollama pull nomic-embed-text
   ```

3. **修改配置**
   ```bash
   # 在.env文件中设置
   SPRING_AI_PROVIDER=ollama
   ```

4. **重启应用**
   ```bash
   mvn spring-boot:run -pl CodeBase-Wiki
   mvn spring-boot:run -pl CodeBase-Review
   ```

### 从本地Ollama切换到阿里云

1. **配置API密钥**
   ```bash
   # 在.env文件中设置
   SPRING_AI_PROVIDER=dashscope
   DASHSCOPE_API_KEY=sk-your-api-key-here
   ```

2. **重启应用**

### 切换到OpenAI

1. **配置OpenAI**
   ```bash
   # 在.env文件中设置
   SPRING_AI_PROVIDER=openai
   OPENAI_API_KEY=sk-your-openai-api-key
   ```

2. **重启应用**

## 模型性能对比

| 特性 | 阿里云DashScope | 本地Ollama | OpenAI |
|------|----------------|------------|--------|
| 部署方式 | 云端API | 本地服务 | 云端API |
| 数据安全 | 云端处理 | 本地处理 | 云端处理 |
| 响应速度 | 网络依赖 | 硬件依赖 | 网络依赖 |
| 使用成本 | 按量付费 | 免费使用 | 按量付费 |
| 模型更新 | 自动更新 | 手动更新 | 自动更新 |
| 硬件要求 | 无要求 | 需要GPU/大内存 | 无要求 |
| 模型质量 | 高 | 中-高 | 非常高 |
| 中文支持 | 优秀 | 良好 | 良好 |
| 代码能力 | 优秀 | 良好 | 优秀 |
| API稳定性 | 高 | 依赖本地 | 非常高 |

## 推荐配置

### 开发环境
- **免费方案**: 本地Ollama：`qwen2.5-coder:14b`
  - 优点：免费、数据安全、离线可用
- **效果优先**: OpenAI：`gpt-4o-mini`
  - 优点：效果好、成本可控、API稳定

### 生产环境
- **中文为主**: 阿里云DashScope：`qwen3-coder-plus-2025-07-22`
  - 优点：稳定可靠、中文优秀、无硬件要求
- **国际项目**: OpenAI：`gpt-4o`
  - 优点：效果最佳、API稳定、全球可用

### 特殊需求
- **数据敏感**: 本地Ollama（完全私有化）
- **成本敏感**: 阿里云DashScope（国内价格优势）
- **效果至上**: OpenAI o1-preview（推理能力最强）

## 故障排除

### Ollama连接失败

```bash
# 检查服务状态
curl http://localhost:11434/api/tags

# 重启服务
ollama serve

# 检查防火墙设置
```

### 模型下载失败

```bash
# 更换下载源
export OLLAMA_HOST=0.0.0.0:11434

# 手动下载
ollama pull qwen2.5-coder:14b --verbose
```

### 内存不足

```bash
# 使用更小的模型
ollama pull qwen2.5-coder:7b
ollama pull llama3.1:8b

# 修改配置
OLLAMA_CHAT_MODEL=qwen2.5-coder:7b
```

## 自定义配置

### 添加新的模型提供商

1. 添加依赖到 `pom.xml`
2. 创建配置类，参考 `MultiLlmConfig.java`
3. 更新 `application.yml` 配置
4. 修改环境变量模板

### 调整模型参数

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: your-custom-model
          temperature: 0.8
```

## 注意事项

1. **切换模型需要重启应用**才能生效
2. **Ollama模型占用较多内存**，建议8GB以上
3. **首次下载模型**可能需要较长时间
4. **生产环境建议使用云端服务**保证稳定性
5. **定期更新本地模型**以获得最佳性能

## 联系支持

如有问题，请：
1. 查看应用日志：`./logs/codebasewiki.log` 或 `./logs/codebasereview.log`
2. 检查Ollama服务状态：`ollama list`
3. 提交Issue到项目仓库
