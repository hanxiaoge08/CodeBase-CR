# Spring AI Alibaba Demo

这是一个使用 Spring AI Alibaba 的示例项目。

## 环境要求

- JDK 21+
- Maven 3.6+
- 阿里云通义千问 API Key

## 配置说明

1. 在运行项目前，请设置环境变量 `DASHSCOPE_API_KEY`，值为您的阿里云通义千问 API Key
2. 默认使用 `qwen-turbo` 模型
3. 服务默认运行在 8080 端口

## 运行项目

```bash
mvn spring-boot:run
```

## API 接口

### 聊天接口

- URL: `/ai/chat`
- 方法: GET
- 参数: 
  - prompt: 聊天提示词（可选，默认值："你好，请介绍一下你自己"）
- 示例: `http://localhost:8080/ai/chat?prompt=你好`

## 注意事项

1. 请确保您有有效的阿里云通义千问 API Key
2. 建议在生产环境中使用更安全的配置管理方式
3. 可以根据需要调整 `application.yml` 中的模型参数 