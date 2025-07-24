# CodeBase-CR - AI代码仓库分析平台

基于Spring AI的智能代码仓库分析平台，集成代码审查、仓库分析、文档生成和任务管理功能，帮助开发团队深度理解和高效管理代码仓库。

## 功能特性

- 🔍 **智能仓库分析**: 深度分析GitHub仓库结构、代码组织和技术架构
- 📚 **AI文档生成**: 基于代码内容自动生成项目文档和技术说明
- 🤖 **智能代码审查**: 集成GitHub Webhook的PR自动审查和建议
- 📊 **任务管理系统**: 完整的分析任务创建、跟踪和管理功能
- 🎯 **精确定位分析**: 提供文件和行级别的详细分析结果
- 🌐 **现代化界面**: React前端提供直观的用户交互体验
- ⚡ **异步处理**: 高性能的后台任务处理和实时状态更新
- 📋 **双知识库**: 结合编码规范和项目上下文的RAG检索增强

## 技术栈

### 后端技术
- **Java 21** + **Spring Boot 3.x** - 核心后端框架
- **Spring AI** + **Spring AI Alibaba** - AI集成和大模型调用
- **SQLite** + **MyBatis-Plus** - 数据持久化
- **GitHub API** + **JGit** - 代码仓库集成
- **阿里云通义千问/OpenAI** - 大语言模型服务

### 前端技术
- **React 18** + **Ant Design 5.x** - 现代化UI框架
- **React Router v6** - 单页应用路由
- **Framer Motion** - 页面动画效果
- **ReactMarkdown** + **Mermaid** - 文档渲染和图表
- **highlight.js** - 代码语法高亮

### 系统架构
- **多模块设计**: CodeBase-Review(审查) + CodeBase-Wiki(分析) + Frontend(界面)
- **异步任务处理**: 基于Spring异步机制的后台任务队列
- **RESTful API**: 前后端分离的接口设计
- **向量检索增强**: RAG技术结合知识库的智能分析

## 快速开始

### 1. 环境准备

确保已安装：
- **Java 21+** (后端)
- **Node.js 16+** + **npm 8+** (前端)
- **Maven 3.8+** (后端构建)
- **Git** (代码仓库操作)

### 2. 配置环境变量

```bash
# AI模型配置（二选一）
export DASHSCOPE_API_KEY=your-dashscope-api-key  # 阿里云通义千问
export OPENAI_API_KEY=your-openai-api-key        # OpenAI

# GitHub集成配置（用于代码审查功能）
export GITHUB_TOKEN=your-github-token
export GITHUB_WEBHOOK_SECRET=your-webhook-secret
```

### 3. 启动后端服务

```bash
# 编译并启动后端
mvn clean install
mvn spring-boot:run -pl CodeBase-Wiki

# 后端将在 http://localhost:8080 启动
```

### 4. 启动前端应用

```bash
# 进入前端目录
cd CodeBaseAI-frontend

# 安装依赖
npm install

# 启动开发服务器
npm start

# 前端将在 http://localhost:3000 启动
```

### 5. 访问应用

- **前端界面**: http://localhost:3000
- **后端API**: http://localhost:8080
- **管理后台**: http://localhost:3000/admin

### 6. 配置GitHub Webhook (可选)

如需使用代码审查功能，在GitHub仓库设置中添加Webhook：
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

## 核心工作流程

### 🔍 仓库分析流程
1. **创建任务**: 通过前端界面创建代码仓库分析任务
2. **获取代码**: 从GitHub克隆仓库或上传项目压缩包
3. **结构分析**: 深度解析项目文件结构和代码组织
4. **AI目录生成**: 基于代码内容智能生成文档目录结构
5. **文档生成**: 使用LLM为每个模块生成详细技术文档
6. **结果展示**: 在前端界面展示分析结果和生成的文档

### 🤖 代码审查流程  
1. **接收事件**: GitHub发送Pull Request事件到Webhook端点
2. **验证签名**: 验证GitHub Webhook签名确保安全性
3. **解析事件**: 提取PR信息并创建审查任务
4. **获取差异**: 通过GitHub API获取PR的diff内容
5. **知识检索**: 使用RAG从知识库中检索相关编码规范
6. **AI审查**: 调用LLM分析代码并生成审查建议
7. **发布结果**: 将审查结果发布为PR评论

### 📊 任务管理流程
1. **任务创建**: 支持Git仓库和文件上传两种方式
2. **异步处理**: 后台队列处理大型项目分析任务  
3. **状态跟踪**: 实时显示任务执行进度和状态
4. **结果查看**: 提供详细的分析报告和文档预览
5. **任务管理**: 支持任务编辑、删除和重新执行

## 项目结构

```
CodeBase-CR/                           # 主项目根目录
├── CodeBase-Review/                   # 代码审查模块
│   ├── src/main/java/com/hxg/crApp/
│   │   ├── adapter/                   # 适配器层
│   │   │   ├── github/               # GitHub API适配器
│   │   │   └── llm/                  # LLM适配器
│   │   ├── controller/               # REST控制器
│   │   ├── service/                  # 业务服务层
│   │   ├── knowledge/                # 知识库管理
│   │   └── dto/                      # 数据传输对象
│   └── src/main/resources/           # 配置和资源文件
│
├── CodeBase-Wiki/                     # 仓库分析模块
│   ├── src/main/java/com/hxg/
│   │   ├── controller/               # API控制器
│   │   ├── service/                  # 分析服务
│   │   ├── entity/                   # 数据实体
│   │   ├── llm/prompt/               # AI提示词模板
│   │   └── mapper/                   # 数据访问层
│   └── src/main/resources/           # SQL脚本和配置
│
├── CodeBaseAI-frontend/               # React前端应用
│   ├── src/
│   │   ├── components/               # 通用组件
│   │   ├── pages/                    # 页面组件
│   │   ├── layouts/                  # 布局组件
│   │   ├── api/                      # API接口
│   │   └── utils/                    # 工具函数
│   ├── public/                       # 静态资源
│   └── package.json                  # 前端依赖配置
│
├── data/                             # 数据存储目录
├── logs/                             # 日志文件目录
├── repository/                       # 代码仓库缓存
└── pom.xml                          # Maven主配置文件
```

### 自定义审查规则

修改相应模块中的提示词配置文件来自定义分析规则。

## 开发指南

### 后端开发

#### 添加新的AI模型支持
1. 在对应模块的 `LlmAdapter` 中添加新的模型客户端
2. 更新 `application.yml` 配置文件
3. 实现对应的提示词模板

#### 扩展分析功能
1. 在 `CodeBase-Wiki` 模块中添加新的分析服务
2. 创建对应的数据库表结构
3. 实现前端展示界面

### 前端开发

#### 添加新页面
1. 在 `src/pages/` 中创建页面组件
2. 在 `App.jsx` 中配置路由
3. 更新导航菜单配置

#### 自定义主题
在 `src/theme/themeConfig.js` 中修改 Ant Design 主题配置。

## 部署指南

### 开发环境部署

#### 后端部署
```bash
# 编译项目
mvn clean package

# 启动服务
java -jar CodeBase-Wiki/target/CodeBase-Wiki-0.0.1-SNAPSHOT.jar
```

#### 前端部署
```bash
cd CodeBaseAI-frontend
npm run build
# 将 build 目录部署到 Web 服务器
```

### 生产环境部署

#### Docker 部署

**后端 Dockerfile**:
```dockerfile
FROM openjdk:21-jdk-slim
COPY CodeBase-Wiki/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**前端 Dockerfile**:
```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY CodeBaseAI-frontend/package*.json ./
RUN npm install
COPY CodeBaseAI-frontend/ .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
EXPOSE 80
```

#### Docker Compose 部署
```yaml
version: '3.8'
services:
  backend:
    build:
      context: .
      dockerfile: Dockerfile.backend
    ports:
      - "8080:8080"
    environment:
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
  
  frontend:
    build:
      context: .
      dockerfile: Dockerfile.frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```

### 生产环境建议

1. **数据库**: 考虑使用 PostgreSQL 替代 SQLite
2. **负载均衡**: 使用 Nginx 或 HAProxy 进行负载均衡
3. **监控**: 集成 Prometheus + Grafana 监控系统
4. **日志**: 使用 ELK Stack 进行日志收集和分析
5. **安全**: 配置 HTTPS 和防火墙规则

## 故障排除

### 常见问题

1. **AI模型调用失败**
   - 检查API密钥配置是否正确
   - 确认网络连接和API配额

2. **前端无法连接后端**
   - 检查API接口地址配置
   - 确认后端服务已正常启动

3. **任务执行失败**
   - 查看日志获取详细错误信息
   - 检查项目文件权限和磁盘空间

4. **GitHub集成问题**
   - 验证GitHub Token权限
   - 检查Webhook配置

### 日志查看

```bash
# 查看应用日志
tail -f logs/application.log

# Docker环境下查看日志
docker logs -f container-name
```

## API文档

### 主要接口

#### 任务管理
- `POST /api/task/upload` - 创建分析任务
- `GET /api/task/list` - 获取任务列表
- `GET /api/task/{taskId}` - 获取任务详情

#### 文档生成
- `GET /api/catalogue/{taskId}` - 获取文档目录
- `GET /api/catalogue/content/{catalogueId}` - 获取文档内容

#### 代码审查
- `POST /api/v1/github/events` - GitHub Webhook接收
- `GET /api/v1/github/health` - 健康检查

## 贡献指南

### 提交代码

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发规范

- 后端遵循 Spring Boot 最佳实践
- 前端使用 ES6+ 和 React Hooks
- 代码注释使用中文
- 提交信息使用中文

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 致谢

感谢以下开源项目：
- [Spring AI](https://spring.io/projects/spring-ai) - AI集成框架
- [Ant Design](https://ant.design/) - React UI组件库
- [阿里云百炼](https://bailian.console.aliyun.com/) - AI模型服务

## 联系方式

如有问题或建议，请：
- 创建 [Issue](https://github.com/your-repo/issues)
- 发送邮件至开发团队
- 加入项目讨论群

---

**CodeBase-CR** - 让代码分析变得智能化 🚀