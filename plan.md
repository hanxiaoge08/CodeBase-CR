# **CodeBase-CR \- 完整开发设计文档**

### **版本: 1.0**

### **技术栈: Java 21, Spring Boot 3.x, Spring AI**

## **1\. 项目概述**

### **1.1. 目标**

构建一个与 GitHub 集成的自动化代码审查工具。该工具通过监听 Pull Request (PR) 事件，调用大语言模型（LLM）结合双知识库（代码规范库、项目上下文库）进行代码分析，并将审查意见以评论形式发布回 PR，旨在提升代码质量与开发效率。

### **1.2. 核心技术栈**

* **后端**: Java 21, Spring Boot 3.x  
* **AI 框架**: Spring AI  
* **LLM 集成**: OpenAI (默认), Ollama (本地)  
* **向量数据库**: ChromaDB (本地开发), Milvus/Postgres+pgvector (生产)  
* **GitHub 集成**: GitHub API (通过 org.kohsuke:github-api), Webhooks  
* **异步处理**: Spring @Async 或 RabbitMQ/Kafka

## **2\. 系统架构**

系统采用事件驱动的微服务架构，核心组件之间解耦，通过异步任务处理代码审查，确保高可用性和可伸缩性。

#### **架构流程图:**

\[GitHub PR Event\] \-\> \[1. Webhook Gateway\] \-\> \[2. Event Processor & Task Queue\] \-\> \[3. AI Review Service\] \-\> \[6. GitHub Comment Publisher\] \-\> \[GitHub PR Comment\]

其中 \[3. AI Review Service\] 依赖于:

* \[4. Knowledge Base Service\]  
  * \[规范知识库 (Vector DB)\]  
  * \[项目上下文知识库 (Vector DB)\]  
* \[5. LLM Service (Spring AI ChatClient)\]

## **3\. 核心组件设计**

1. **Webhook Gateway (网关)**  
   * **职责**: 提供公开的 HTTP 端点，接收并验证 GitHub pull\_request Webhook 事件。  
   * **实现**: Spring MVC @RestController。包含使用 X-Hub-Signature-256 头进行签名的安全验证逻辑。  
2. **Event Processor & Task Queue (事件处理器与任务队列)**  
   * **职责**: 解析 Webhook 负载，提取关键信息（仓库、PR号、diff URL），封装成 ReviewTask 对象，并将其放入异步处理队列。  
   * **实现**: 一个 Spring @Service。简单实现可使用 @Async，生产环境推荐使用消息队列（如 RabbitMQ）来保证任务的可靠性。  
3. **AI Review Service (AI审查服务)**  
   * **职责**: 系统的核心，消费 ReviewTask，编排整个 RAG (Retrieval-Augmented Generation) 审查流程。  
   * **实现**: 核心 @Service，负责调用下游服务获取代码、检索知识、与 LLM 交互。  
4. **Knowledge Base Service (知识库服务)**  
   * **职责**: 管理和查询向量知识库。  
   * **实现**:  
     * **规范知识库**: 应用启动时，加载《阿里巴巴Java开发手册》等文档，通过 Spring AI 的 DocumentReader, TextSplitter, EmbeddingClient 进行处理并存入 VectorStore。  
     * **项目上下文知识库**: 首次审查某项目时，异步克隆项目代码，扫描 .java 文件，同样进行向量化存储。此知识库需与项目关联。  
5. **LLM Service (大语言模型服务)**  
   * **职责**: 封装与大模型的交互。  
   * **实现**: 依赖 Spring AI 自动配置的 ChatClient。使用 PromptTemplate 动态构建包含 RAG 检索结果的提示词，并使用 BeanOutputParser 将 LLM 返回的 JSON 结果自动映射到 Java 对象。  
6. **GitHub Comment Publisher (评论发布器)**  
   * **职责**: 将格式化的审查意见通过 GitHub API 发布到 PR。  
   * **实现**: 使用 github-api 库，处理好 API 认证（推荐使用 GitHub App Token）和调用逻辑，能将评论发布到文件具体行或整个 PR。

## **4\. 核心工作流程 (RAG-Based Review)**

1. **触发**: 用户在 GitHub 创建或更新一个 Pull Request。  
2. **接收**: Webhook Gateway 接收到事件，验证通过后将任务信息发送到队列。  
3. **消费**: AI Review Service 从队列中获取一个审查任务。  
4. **获取代码**: 服务通过 GitHub API 获取 PR 的 diff 内容。  
5. **检索 (Retrieval)**:  
   * 解析 diff，确定审查的关键代码片段。  
   * 针对每个片段，在 **规范知识库** 和 **项目上下文知识库** 中执行向量相似度搜索，分别检索最相关的规范条例和现有代码示例。  
6. **增强 (Augmentation)**:  
   * 构建一个精密的 Prompt。将检索到的规范条例、代码示例、以及待审查的 diff 内容填入 PromptTemplate。  
   * 利用 BeanOutputParser，在 Prompt 中明确要求 LLM 以指定的 JSON 格式返回结果。  
7. **生成 (Generation)**:  
   * 调用 ChatClient 将完整的 Prompt 发送给 LLM。  
   * LLM 根据丰富的上下文信息，生成结构化的代码审查意见（例如，\[{filePath, lineNumber, comment}\]）。  
8. **发布**: GitHub Comment Publisher 将解析后的审查意见逐条发布到 PR 的相应位置。

## **5\. 关键数据模型 (Java Records)**

```java
// 审查任务  
public record ReviewTask(  
    String repositoryName,  
    int prNumber,  
    String diffUrl  
) {}

// 单条审查意见 (用于LLM输出解析)  
public record ReviewComment(  
    String filePath,    // 文件路径  
    int lineNumber,     // 代码行号  
    String comment      // 审查意见  
) {}

// 审查意见列表 (用于BeanOutputParser)  
public record ReviewCommentList(  
    java.util.List\<ReviewComment\> comments  
) {}
```



## **6\. 开发里程碑**

* **M1: 核心框架与 GitHub 通信 (1-2周)**  
  * 完成项目初始化，实现 Webhook 接收、验证及异步任务分发。  
  * 成功拉取 PR 的 diff 内容。  
  * **交付**: 一个能响应 PR 并记录 diff 的基础服务。  
* **M2: 无 RAG 的基础 AI 审查 (2-3周)**  
  * 集成 ChatClient，实现一个简单的 Prompt 直接对 diff 提问。  
  * 实现 Comment Publisher，能将 AI 返回的文本作为全局评论发布。  
  * **交付**: 能对 PR 进行简单、笼统的 AI 审查。  
* **M3: 实现完整的 RAG 审查流程 (3-4周)**  
  * 完成双知识库的构建与查询逻辑。  
  * 实现复杂的 Prompt 工程与 BeanOutputParser 结构化输出。  
  * 将评论发布到具体代码行。  
  * **交付**: 工具核心功能完成，能提供具有上下文的、精确的代码行级审查。  
* **M4: 优化、部署与监控 (1-2周)**  
  * 完善配置、日志、错误处理。  
  * 容器化 (Dockerfile) 并编写部署脚本。  
  * **交付**: 产品的 V1.0 版本。

## **7\. 风险与应对**

* **LLM 成本与延迟**:  
  * **应对**: 优先使用速度快、成本低的模型；优化 Prompt 减少 token 消耗；探索对 diff 进行预处理，只审查关键部分。  
* **审查质量不稳定 (幻觉)**:  
  * **应对**: 强化 Prompt 的约束性指令；通过高质量的 RAG 数据减少模型自由发挥的空间；明确标注 AI 评论，供人类参考。  
* **上下文知识库构建开销大**:  
  * **应对**: 首次构建后，后续仅对变更文件进行增量更新索引；将构建任务置于低峰期执行。  
* **GitHub API 速率限制**:  
  * **应对**: 使用 GitHub App 认证获取更高配额；在 API 客户端中实现缓存和智能重试机制。