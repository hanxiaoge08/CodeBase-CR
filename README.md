# CodeBase-CR - AI代码仓库分析平台

基于Spring AI的智能代码仓库分析平台，集成代码审查、仓库分析、文档生成和任务管理功能，帮助开发团队深度理解和高效管理代码仓库。

<img width="2545" height="1129" alt="image-20250811094915448" src="https://github.com/user-attachments/assets/2b5ee8e1-e5cb-4205-bbde-171280136ce6" />
<img width="2520" height="1134" alt="image-20250811094958671" src="https://github.com/user-attachments/assets/130f8fc6-5b80-49e1-8391-69a5c471a93d" />

## 功能特性

- 🔍 **智能仓库分析**: 深度分析GitHub仓库结构、代码组织和技术架构
- 📚 **AI文档生成**: 基于代码内容自动生成项目文档和技术说明
- 🤖 **智能代码审查**: 1+4+1多智能体工作流，包含风格、逻辑、安全等全方位审查
- 📊 **任务管理系统**: 完整的分析任务创建、跟踪和管理功能
- 🎯 **精确代码切片**: 基于Tree-sitter的AST解析服务，提供函数级代码分块
- 🌐 **现代化界面**: React前端提供直观的用户交互体验
- ⚡ **异步处理**: 基于Kafka消息队列的高性能后台任务处理
- 🔍 **混合检索增强**: Elasticsearch BM25+kNN+RRF融合的增强混合检索
- 🚀 **并发控制**: 智能的任务并发限制和API访问频率控制
- 🧠 **智能上下文**: 基于任务级数据隔离的RAG检索增强生成
- 💬 **Chat to Repository**: 支持与代码仓库的智能对话交互

## 技术栈

### 后端技术
- **Java 21** + **Spring Boot 3.4.5** - 核心后端框架
- **Spring AI1.0.0** + **Spring AI Alibaba1.0.0.3** - AI集成和大模型调用
- **Apache Kafka** - 消息队列系统，支持异步文档生成和Code Review
- **Elasticsearch 8.15.0** - 混合检索引擎，支持BM25+kNN+RRF融合算法
- **SQLite** + **MyBatis-Plus** - 数据持久化
- **GitHub API** + **JGit** - 代码仓库集成
- **百炼平台Qwen3** - 大语言模型服务
- **Tree-sitter** + **FastAPI** - 代码AST解析和切片服务

### 前端技术
- **React 18** + **Ant Design 5.x** - 现代化UI框架
- **React Router v6** - 单页应用路由
- **Framer Motion** - 页面动画效果
- **ReactMarkdown** + **Mermaid** - 文档渲染和图表
- **highlight.js** - 代码语法高亮

### 系统架构
- **多模块设计**: CodeBase-Review(审查) + CodeBase-Wiki(分析) + AST服务(切片) + Frontend(界面)
- **Kafka异步架构**: 生产者-消费者模式的消息队列处理
- **混合检索架构**: Elasticsearch BM25文本检索 + kNN向量检索 + RRF融合算法
- **多智能体工作流**: 1+4+1架构，包含协调、初筛、并行专家分析、汇总合成
- **RESTful API**: 前后端分离的接口设计
- **AST代码解析**: Tree-sitter支持多语言代码结构化分析

## 快速开始

### 1. 环境准备

确保已安装：
- **Java 21+** (后端)
- **Node.js 16+** + **npm 8+** (前端)  
- **Maven 3.8+** (后端构建)
- **Git** (代码仓库操作)
- **Docker** (可选，用于Kafka部署)

### 2. 配置环境变量

**重要提醒**: 项目已将敏感配置信息迁移到环境变量，请参考完整的[环境变量配置文档](ENVIRONMENT_VARIABLES.md)进行配置。

**🔥 新功能**: 现已支持三种大语言模型提供商：
- **阿里云DashScope**（默认）：中文优化，价格合理
- **本地Ollama**：完全免费，数据私有  
- **OpenAI**：效果顶级，API稳定

详细配置请参考[多大模型支持文档](MULTI_LLM_SUPPORT.md)。

#### 快速配置
```bash
# 1. 复制环境变量模板
cp environment-template.env .env

# 2. 编辑 .env 文件，设置必需的环境变量
# 🔥 新功能：多大模型支持
export SPRING_AI_PROVIDER=dashscope                 # 大模型提供商: dashscope(默认) | ollama(本地) | openai

# 阿里云DashScope配置（使用云端模型时必需）
export DASHSCOPE_API_KEY=your-dashscope-api-key     # 阿里云大模型API密钥

# 本地Ollama配置（使用本地模型时必需）
export OLLAMA_BASE_URL=http://localhost:11434       # Ollama服务地址
export OLLAMA_CHAT_MODEL=qwen2.5-coder:14b          # 推荐的代码专用模型

# OpenAI配置（使用OpenAI模型时必需）
export OPENAI_API_KEY=sk-your-openai-api-key        # OpenAI API密钥
export OPENAI_CHAT_MODEL=gpt-4o                     # 推荐的OpenAI模型

# GitHub集成配置
export GITHUB_TOKEN=your-github-token               # GitHub访问令牌
export GITHUB_WEBHOOK_SECRET=your-webhook-secret    # GitHub Webhook密钥

# 通知服务配置（如需使用）
export FEISHU_WIKI_WEBHOOK=your-feishu-webhook      # 飞书机器人
export DINGTALK_WIKI_WEBHOOK=your-dingtalk-webhook  # 钉钉机器人

# 其他服务配置（有默认值）
export REDIS_HOST=your-redis-host                   # Redis服务器
export REDIS_PASSWORD=your-redis-password           # Redis密码
export KAFKA_BOOTSTRAP_SERVERS=your-kafka-servers   # Kafka集群
export ELASTICSEARCH_URIS=your-es-uris              # Elasticsearch集群
```

#### 配置说明
- 📋 **完整配置清单**: 查看 [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) 获取所有环境变量的详细说明
- 📝 **模板文件**: 使用 `environment-template.env` 作为配置模板
- 🔐 **安全注意**: 不要将包含真实密钥的 `.env` 文件提交到版本控制

### 3. 启动AST代码解析服务

```bash
# 进入AST服务目录
cd ast

# 创建虚拟环境（推荐）
python -m venv venv
source venv/bin/activate  # Linux/Mac
# 或 venv\Scripts\activate  # Windows

# 安装依赖
pip install -r requirements.txt

# 启动服务（后台运行）
nohup python -m uvicorn server:app --host 0.0.0.0 --port 8566 --workers 2 > ast.txt 2>&1 & echo $! > ast.pid

# 检查服务状态
curl http://localhost:8566/health
```

### 4. 启动Kafka服务

```bash
# 使用Docker Compose启动Kafka
cd CodeBase-Wiki
docker-compose up -d
```
### 查看初始化日志
```bash
docker-compose logs kafka-init
```

### 验证 topics 是否创建成功
```bash
# 使用 docker exec 查看
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# 或者访问 AKHQ 管理界面
# http://localhost:8081
```

### 5. 启动Elasticsearch服务

```bash
# 使用Docker启动Elasticsearch 8.15.0
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
  elasticsearch:8.15.0

# 检查服务状态
curl http://localhost:9200
```

### 6. 新建SQLite数据库

```bash
# 创建数据目录
mkdir -p data

# 数据库会在首次启动时自动创建
# 位置：data/codebasewiki_db.sqlite
```

### 7. 启动后端服务

```bash

ngrok http 8086
# 推荐启动顺序：
# 1. AST服务 (端口8566)
# 2. Elasticsearch (端口9200) 
# 3. Kafka (端口9092)
# 4. CodeBase-Wiki服务 (端口8085)
# 5. CodeBase-Review服务 (端口8086)

# 使用IDE启动或Maven命令
mvn spring-boot:run -pl CodeBase-Wiki
mvn spring-boot:run -pl CodeBase-Review
```

### 8. 启动前端应用

```bash
# 进入前端目录
cd CodeBaseAI-frontend

# 安装依赖
npm install

# 启动开发服务器
npm start

# 前端将在 http://localhost:3000 启动
# 前端需要强制跨域（在chrome快捷方式属性中目标最后加入 --args --disable-web-security --user-data-dir=<空白存储目录用于存储数据>)
```

### 9. 访问应用

- **前端界面**: http://localhost:3000
- **Wiki服务**: http://localhost:8085
- **Review服务**: http://localhost:8086
- **AST解析服务**: http://localhost:8566
- **Elasticsearch**: http://localhost:9200
- **Kafka管理界面**: http://localhost:8001 (AKHQ)

### 10. 配置GitHub Webhook (可选)

本地调试时，可使用ngrok代理本地端口
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
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen3-coder-plus-2025-07-22

# CodeBase-Wiki模块专用配置
project:
  wiki:
    prompt:
      doc-version: v3    # 文档生成提示词版本
    kafka:
      topics:
        doc-generation: "wiki-doc-generation"      # 主队列
        doc-retry: "wiki-doc-retry"                # 重试队列
        doc-dlq: "wiki-doc-dlq"                    # 死信队列
      consumer:
        max-concurrency: 2        # 最大并发处理数
        process-interval: 2000    # 处理间隔(毫秒)
        max-retry: 3              # 最大重试次数

# GitHub配置
app:
  github:
    token: ${GITHUB_TOKEN}
    webhook:
      secret: ${GITHUB_WEBHOOK_SECRET}

# 记忆系统配置
memory-service:
  enabled: true
  base-url: ${MEM0_API_URL}
```

## 核心工作流程

### 🔍 仓库分析流程
1. **创建任务**: 通过前端界面创建代码仓库分析任务
2. **获取代码**: 从GitHub克隆仓库或上传项目压缩包
3. **代码切片**: 使用AST解析服务将代码切分为函数级别的结构化块
4. **结构分析**: 深度解析项目文件结构和代码组织关系
5. **AI目录生成**: 基于代码内容智能生成文档目录结构
6. **异步文档生成**: 通过Kafka消息队列异步生成每个目录的详细文档
7. **混合索引**: 将代码块和文档索引到Elasticsearch，支持BM25+kNN检索
8. **状态同步**: 实时更新任务状态和进度
9. **结果展示**: 在前端界面展示分析结果和生成的文档

### 🤖 代码审查流程 (1+4+1多智能体架构)
1. **接收事件**: GitHub发送Pull Request事件到Webhook端点
2. **ReviewCoordinatorAgent**: 解析PR元数据、提取diff统计信息
3. **TriageAgent**: 初筛和路由决策，判断审查类型和复杂度
4. **并行专家分析**: 
   - **StyleConventionAgent**: 编码规范检查（集成RAG检索）
   - **LogicContextAgent**: 逻辑上下文分析（基于AST解析）
   - **SecurityScanAgent**: 安全漏洞扫描
5. **ReportSynthesizerAgent**: 汇总各专家分析结果
6. **发布结果**: 将综合评审报告发布为PR评论

### 📊 任务管理流程
1. **任务创建**: 支持Git仓库和文件上传两种方式
2. **消息发送**: 将文档生成任务发送到Kafka主队列
3. **异步处理**: 消费者从队列中拉取任务并处理
4. **任务验证**: 处理前验证任务和目录记录是否仍存在
6. **状态跟踪**: 实时显示任务执行进度和状态
7. **错误重试**: 失败任务自动重试（最大3次）
8. **死信处理**: 超过重试次数的任务进入死信队列
9. **任务管理**: 支持任务编辑、删除和重新执行

### 🚀 Kafka消息队列架构
```
生产者 -> [主队列] -> 消费者 -> 处理成功
    |              |
    v              v (失败)
[重试队列] <----- 重试逻辑
    |
    v (超过重试次数)
[死信队列]
```

## 项目结构

```
CodeBase-CR/                           # 主项目根目录
├── CodeBase-Review/                   # 代码审查模块
│   ├── src/main/java/com/way/crApp/
│   │   ├── adapter/                   # 适配器层
│   │   │   ├── github/               # GitHub API适配器
│   │   │   └── llm/                  # LLM适配器
│   │   ├── agent/                    # 多智能体工作流
│   │   │   ├── ReviewCoordinatorAgent.java    # 协调代理
│   │   │   ├── TriageAgent.java              # 初筛代理
│   │   │   ├── StyleConventionAgent.java     # 风格检查
│   │   │   ├── LogicContextAgent.java        # 逻辑分析
│   │   │   ├── SecurityScanAgent.java        # 安全扫描
│   │   │   └── ReportSynthesizerAgent.java   # 报告合成
│   │   ├── controller/               # REST控制器
│   │   ├── service/                  # 业务服务层
│   │   ├── client/                   # ES Wiki服务客户端
│   │   └── dto/                      # 数据传输对象
│   └── src/main/resources/           # 配置和资源文件
│
├── CodeBase-Wiki/                     # 仓库分析模块
│   ├── src/main/java/com/way/
│   │   ├── controller/               # API控制器
│   │   ├── service/                  # 分析服务
│   │   │   ├── impl/                 # 服务实现类
│   │   │   └── EnhancedHybridSearchService.java  # 混合检索服务
│   │   ├── queue/                    # Kafka消息队列层
│   │   │   ├── config/               # Kafka配置
│   │   │   ├── producer/             # 消息生产者
│   │   │   ├── consumer/             # 消息消费者
│   │   │   ├── service/              # 文档处理服务
│   │   │   └── model/                # 消息模型
│   │   ├── entity/                   # 数据实体
│   │   ├── llm/                      # AI集成层
│   │   │   ├── prompt/               # AI提示词模板
│   │   │   ├── service/              # LLM服务
│   │   │   └── tool/                 # AI工具（FileSystemTool等）
│   │   ├── config/                   # Elasticsearch配置
│   │   └── mapper/                   # 数据访问层
│   ├── docker-compose.yml            # Kafka Docker配置
│   ├── start-kafka.ps1               # Kafka启动脚本
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
├── ast/                              # AST代码解析服务
│   ├── server.py                     # FastAPI服务主文件
│   ├── requirements.txt              # Python依赖
│   └── README.md                     # AST服务说明
│
├── data/                             # 数据存储目录
├── logs/                             # 日志文件目录
├── repository/                       # 代码仓库缓存
├── environment-template.env          # 环境变量模板
├── ENVIRONMENT_VARIABLES.md          # 环境变量配置文档
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
   - 查看日志中的具体错误信息

2. **前端无法连接后端**
   - 检查API接口地址配置
   - 确认后端服务已正常启动
   - 检查CORS配置

3. **任务执行失败**
   - 查看日志获取详细错误信息
   - 检查项目文件权限和磁盘空间
   - 确认依赖服务（Kafka、Mem0）状态

4. **Kafka连接问题**
   - 确认Kafka服务已启动：`docker-compose ps`
   - 检查端口占用：`netstat -an | findstr 9092`
   - 查看Kafka日志：`docker logs kafka`

5. **FileSystemTool重复读取文件**
   - 检查ThreadLocal清理是否正常
   - 重启应用清除可能的状态污染
   - 查看文件读取缓存状态

6. **任务删除后仍在执行**
   - 确认已更新到最新版本（包含任务验证逻辑）
   - 查看消费者日志确认跳过删除任务的记录
   - 检查数据库中任务记录是否已删除

7. **GitHub集成问题**
   - 验证GitHub Token权限
   - 检查Webhook配置和密钥
   - 查看GitHub事件接收日志

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

## 更新日志

### v3.0.0 (2025-01-XX)
- 🤖 **多智能体架构**: 实现1+4+1代码审查工作流，包含协调、初筛、并行专家分析、汇总合成
- 🔍 **混合检索升级**: 替换Mem0为Elasticsearch 8.15.0，支持BM25+kNN+RRF融合算法
- 🎯 **AST代码解析**: 新增Tree-sitter基础的代码切片服务，支持函数级结构化分析
- 🏗️ **架构重构**: 数据字段规范化，repoId和taskId概念分离，支持任务级数据隔离
- 🔐 **安全增强**: 所有敏感配置迁移到环境变量，提供完整的配置模板和文档
- 📈 **性能优化**: RRF融合算法提升检索质量，TopK策略优化上下文生成
- 🛠️ **服务解耦**: Review模块通过ES Wiki Service Client调用Wiki检索服务
- 📚 **文档完善**: 提供详细的混合检索、多智能体、AST解析等功能文档

### v2.0.0 (2025-08-06)
- 🚀 **重大架构升级**: 从Spring异步改为Kafka消息队列架构
- ⚡ **并发控制优化**: 实现基于信号量的精确并发限制（2个并发任务）
- 🔧 **FileSystemTool修复**: 解决ThreadLocal状态污染和重复文件读取问题
- 🛡️ **任务删除优化**: 修复删除任务后消息队列仍执行的问题
- 📊 **队列监控**: 添加Kafka队列状态监控和AKHQ管理界面
- 🔄 **重试机制**: 实现3次重试+死信队列的容错处理
- 🐳 **Docker支持**: 提供完整的Docker Compose部署方案

### v1.0.0 (2025-07-31)  
- ✨ 初始版本发布
- 🔍 基于Spring AI的智能代码仓库分析
- 📚 LLM自动文档生成功能
- 🤖 GitHub PR代码审查集成
- 🎯 React前端界面

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 致谢

感谢以下开源项目：
- [Spring AI](https://spring.io/projects/spring-ai) - AI集成框架
- [Apache Kafka](https://kafka.apache.org/) - 分布式消息队列
- [Elasticsearch](https://www.elastic.co/) - 搜索和分析引擎
- [Tree-sitter](https://tree-sitter.github.io/) - 代码解析库
- [FastAPI](https://fastapi.tiangolo.com/) - 现代Python Web框架
- [Ant Design](https://ant.design/) - React UI组件库  
- [阿里云百炼](https://bailian.console.aliyun.com/) - AI模型服务

## 联系方式

如有问题或建议，请：
- 创建 [Issue]
- 发送邮件至开发团队
- 加入项目讨论群

---

**CodeBase-CR** - 让代码分析变得智能化 🚀