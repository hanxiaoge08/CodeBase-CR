//package com.hxg.crApp.service.impl;
//
//import com.hxg.crApp.service.port.IKnowledgeBaseService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.reader.TextReader;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.ResourceLoader;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 知识库服务实现
// *
// * 管理规范知识库和项目上下文知识库的构建与查询
// */
//@Service
//public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService {
//
//    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);
//
//    @Autowired
//    private VectorStore vectorStore;
//
//    @Autowired
//    private EmbeddingModel embeddingModel;
//
//    @Autowired
//    private ResourceLoader resourceLoader;
//
//    @Value("${app.knowledge.base-path:./knowledge-base}")
//    private String knowledgeBasePath;
//
//    // 记录已构建的知识库
//    private final Set<String> builtStyleGuides = ConcurrentHashMap.newKeySet();
//    private final Set<String> builtProjects = ConcurrentHashMap.newKeySet();
//
//    @Override
//    public void buildStyleGuideKnowledgeBase() {
//        if (builtStyleGuides.contains("default")) {
//            logger.info("规范知识库已存在，跳过构建");
//            return;
//        }
//
//        try {
//            logger.info("开始构建规范知识库...");
//
//            // 创建默认的Java编码规范文档
//            List<Document> styleGuideDocuments = createDefaultStyleGuideDocuments();
//
//            if (!styleGuideDocuments.isEmpty()) {
//                // 文本分割
//                TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 5, 10000, true);
//                List<Document> splitDocuments = splitter.apply(styleGuideDocuments);
//
//                // 添加到向量存储
//                vectorStore.add(splitDocuments);
//
//                builtStyleGuides.add("default");
//                logger.info("规范知识库构建完成，文档数量: {}", splitDocuments.size());
//            } else {
//                logger.warn("未找到规范文档，创建基础规范知识库");
//                builtStyleGuides.add("default");
//            }
//
//        } catch (Exception e) {
//            logger.error("构建规范知识库时发生错误", e);
//        }
//    }
//
//    @Override
//    public void buildOrUpdateProjectKnowledgeBase(String repoFullName) {
//        if (builtProjects.contains(repoFullName)) {
//            logger.info("项目知识库已存在: {}", repoFullName);
//            return;
//        }
//
//        try {
//            logger.info("开始构建项目知识库: {}", repoFullName);
//
//            // 创建项目上下文文档（简化版，实际应该克隆仓库并扫描代码）
//            List<Document> projectDocuments = createProjectContextDocuments(repoFullName);
//
//            if (!projectDocuments.isEmpty()) {
//                // 文本分割
//                TokenTextSplitter splitter = new TokenTextSplitter(800, 100, 5, 10000, true);
//                List<Document> splitDocuments = splitter.apply(projectDocuments);
//
//                // 添加到向量存储
//                vectorStore.add(splitDocuments);
//
//                builtProjects.add(repoFullName);
//                logger.info("项目知识库构建完成: {}, 文档数量: {}", repoFullName, splitDocuments.size());
//            } else {
//                logger.warn("未找到项目代码，跳过知识库构建: {}", repoFullName);
//                builtProjects.add(repoFullName); // 标记为已处理，避免重复尝试
//            }
//
//        } catch (Exception e) {
//            logger.error("构建项目知识库时发生错误: {}", repoFullName, e);
//        }
//    }
//
//    @Override
//    public boolean isStyleGuideKnowledgeBaseBuilt() {
//        return builtStyleGuides.contains("default");
//    }
//
//    @Override
//    public boolean isProjectKnowledgeBaseBuilt(String repoFullName) {
//        return builtProjects.contains(repoFullName);
//    }
//
//    /**
//     * 创建默认的编码规范文档
//     *
//     * @return 规范文档列表
//     */
//    private List<Document> createDefaultStyleGuideDocuments() {
//        List<Document> documents = new ArrayList<>();
//
//        try {
//            // 从classpath加载规范文档
//            Resource resource = resourceLoader.getResource("classpath:knowledge/java-style-guide.txt");
//            if (resource.exists()) {
//                TextReader textReader = new TextReader(resource);
//                documents.addAll(textReader.get());
//            } else {
//                // 如果没有外部文档，创建基础的编码规范
//                documents.add(createBasicStyleGuideDocument());
//            }
//        } catch (Exception e) {
//            logger.warn("加载规范文档失败，使用默认规范: {}", e.getMessage());
//            documents.add(createBasicStyleGuideDocument());
//        }
//
//        return documents;
//    }
//
//    /**
//     * 创建基础的编码规范文档
//     *
//     * @return 基础规范文档
//     */
//    private Document createBasicStyleGuideDocument() {
//        String basicStyleGuide = """
//            Java 编码规范要点：
//
//            1. 命名规范：
//            - 类名使用PascalCase，如：UserService, OrderController
//            - 方法名和变量名使用camelCase，如：getUserById, userName
//            - 常量使用UPPER_SNAKE_CASE，如：MAX_RETRY_COUNT
//            - 包名使用小写，如：com.company.module
//
//            2. 代码结构：
//            - 每个类应该有单一职责
//            - 方法应该简短，一般不超过30行
//            - 避免过深的嵌套，建议不超过3层
//            - 使用有意义的变量名和方法名
//
//            3. 注释规范：
//            - 公开的API必须有JavaDoc注释
//            - 复杂的业务逻辑需要添加行内注释
//            - 注释应该说明"为什么"而不是"怎么做"
//
//            4. 异常处理：
//            - 不要忽略异常
//            - 使用具体的异常类型
//            - 记录异常日志
//
//            5. 性能考虑：
//            - 避免在循环中创建不必要的对象
//            - 使用StringBuilder进行字符串拼接
//            - 合理使用缓存
//            - 注意数据库查询性能
//
//            6. 安全规范：
//            - 验证所有用户输入
//            - 防止SQL注入
//            - 敏感信息不要硬编码
//            """;
//
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("source", "basic-java-style-guide");
//        metadata.put("type", "style_guide");
//        metadata.put("category", "java-coding-standards");
//
//        return new Document(basicStyleGuide, metadata);
//    }
//
//    /**
//     * 创建项目上下文文档（简化版）
//     *
//     * @param repoFullName 仓库全名
//     * @return 项目文档列表
//     */
//    private List<Document> createProjectContextDocuments(String repoFullName) {
//        List<Document> documents = new ArrayList<>();
//
//        try {
//            // 这里应该实现实际的代码克隆和扫描逻辑
//            // 目前先创建一个占位符文档
//            String projectInfo = String.format("""
//                项目: %s
//
//                这是一个Java项目的上下文信息占位符。
//                在完整实现中，这里应该包含：
//                1. 项目的代码结构信息
//                2. 常用的设计模式
//                3. 项目特定的编码约定
//                4. 核心类和方法的示例
//
//                由于当前是简化实现，实际的代码扫描功能需要在后续版本中完善。
//                """, repoFullName);
//
//            Map<String, Object> metadata = new HashMap<>();
//            metadata.put("source", "project-context");
//            metadata.put("type", "project_code");
//            metadata.put("repository", repoFullName);
//
//            documents.add(new Document(projectInfo, metadata));
//
//        } catch (Exception e) {
//            logger.error("创建项目上下文文档时发生错误: {}", repoFullName, e);
//        }
//
//        return documents;
//    }
//}