# CodeBase-Review 多智能体代码审查系统

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- 阿里云百炼账号
- GitHub Token

### 配置步骤

1. **设置环境变量**
```bash
export DASHSCOPE_API_KEY=sk-xxx        # 阿里云百炼API密钥
export GITHUB_TOKEN=ghp_xxx            # GitHub访问令牌
export GITHUB_WEBHOOK_SECRET=xxx       # Webhook密钥（可选）
```

2. **启动应用**
```bash
mvn spring-boot:run
```

3. **配置GitHub Webhook**
- URL: `https://your-domain/api/v1/github/events`
- Events: Pull requests
- Secret: 与环境变量中的GITHUB_WEBHOOK_SECRET一致

## 📋 系统架构

### 多智能体工作流（1+4+1模式）
```
START → ReviewCoordinator → Triage → ParallelStarter
                                           ↓
                        ┌──────────────────┼──────────────┐
                        ↓                  ↓              ↓
                 StyleAgent          LogicAgent    SecurityAgent
                        ↓                  ↓              ↓
                        └──────────────────┼──────────────┘
                                           ↓
                                  ReportSynthesizer → END
```

### 核心Agent
- **ReviewCoordinatorAgent**: PR元数据解析
- **TriageAgent**: 初步评估和路由决策
- **StyleConventionAgent**: 编码规范审查（集成RAG）
- **LogicContextAgent**: 逻辑审查（集成Memory服务）
- **SecurityScanAgent**: 安全漏洞扫描
- **ReportSynthesizerAgent**: 结果汇总和报告生成

## 🔧 主要特性

- ✅ **多智能体协作**：6个专业Agent各司其职
- ✅ **并行处理**：三个专家Agent同时审查，提高效率
- ✅ **知识库增强**：集成RAG服务，提供上下文感知
- ✅ **Memory服务集成**：项目历史记忆，深度理解代码
- ✅ **智能路由**：根据PR特征动态决定审查策略
- ✅ **降级容错**：失败时自动降级到传统流程

## 📊 任务状态

### 已完成
- ✅ 多智能体系统核心实现
- ✅ Spring AI Alibaba Graph集成
- ✅ Webhook与Graph工作流集成
- ✅ 知识库查询集成
- ✅ 技术文档编写

### 待完成（按优先级）
- ⏳ 配置外部化（高）
- ⏳ GitHub API限流处理（高）
- ⏳ 大型PR分块处理（高）
- ⏳ 缓存机制（中）
- ⏳ 监控和可观测性（中）
- ⏳ 测试覆盖（中低）
- ⏳ 历史查询功能（低）

## 📖 文档

详细技术文档请参考：[技术文档.md](技术文档.md)

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 License

MIT License