//package com.hxg.crApp.knowledge;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * RAG服务
// * <p>
// * 职责：执行检索增强生成（RAG）中的"检索"步骤
// * 核心方法：retrieveContext(String codeSnippet, String repoFullName)
// * 流程：接收一小段代码 -> 在规范库和项目上下文中分别执行向量搜索 -> 返回最相关的文本片段
// */
//@Service
//public class RAGService {
//
//    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);
//
//    @Autowired
//    private ElasticsearchVectorStore elasticsearchVectorStore;
//
//    // 向量检索的相关常量
//    private static final int TOP_K = 5; // 检索top-k个最相关的文档
//    private static final double SIMILARITY_THRESHOLD = 0.7; // 相似度阈值
//
//    /**
//     * 检索相关上下文信息
//     *
//     * @param codeSnippet  代码片段
//     * @param repoFullName 仓库全名
//     * @return 相关的上下文信息
//     */
//    public String retrieveContext(String codeSnippet, String repoFullName) {
//        try {
//            logger.debug("开始检索上下文: repo={}", repoFullName);
//
//            // 创建搜索请求
//            SearchRequest searchRequest = SearchRequest.builder().query(codeSnippet)
//                    .topK(TOP_K)
//                    .similarityThreshold(SIMILARITY_THRESHOLD)
//                    .build();
//
//            // 执行向量搜索
//            List<Document> relevantDocs = elasticsearchVectorStore.similaritySearch(searchRequest);
//
//            if (relevantDocs.isEmpty()) {
//                logger.debug("未找到相关上下文文档: repo={}", repoFullName);
//                return "暂无相关上下文信息";
//            }
//
//            // 分类处理检索到的文档
//            String styleGuideContext = extractStyleGuideContext(relevantDocs);
//            String projectContext = extractProjectContext(relevantDocs, repoFullName);
//
//            // 组装上下文信息
//            StringBuilder contextBuilder = new StringBuilder();
//
//            if (!styleGuideContext.isEmpty()) {
//                contextBuilder.append("**相关编码规范:**\n")
//                        .append(styleGuideContext)
//                        .append("\n\n");
//            }
//
//            if (!projectContext.isEmpty()) {
//                contextBuilder.append("**项目代码示例:**\n")
//                        .append(projectContext)
//                        .append("\n\n");
//            }
//
//            String finalContext = contextBuilder.toString().trim();
//            logger.debug("检索到上下文信息: repo={}, 文档数={}, 字符长度={}",
//                    repoFullName, relevantDocs.size(), finalContext.length());
//
//            return finalContext.isEmpty() ? "暂无相关上下文信息" : finalContext;
//
//        } catch (Exception e) {
//            logger.error("检索上下文时发生错误: repo={}", repoFullName, e);
//            return "检索上下文时发生错误";
//        }
//    }
//
//    /**
//     * 提取编码规范相关的上下文
//     *
//     * @param documents 检索到的文档
//     * @return 编码规范上下文
//     */
//    private String extractStyleGuideContext(List<Document> documents) {
//        return documents.stream()
//                .filter(doc -> isStyleGuideDocument(doc))
//                .map(Document::getText)
//                .limit(3) // 限制数量避免上下文过长
//                .collect(Collectors.joining("\n---\n"));
//    }
//
//    /**
//     * 提取项目代码相关的上下文
//     *
//     * @param documents    检索到的文档
//     * @param repoFullName 仓库全名
//     * @return 项目代码上下文
//     */
//    private String extractProjectContext(List<Document> documents, String repoFullName) {
//        return documents.stream()
//                .filter(doc -> isProjectDocument(doc, repoFullName))
//                .map(Document::getText)
//                .limit(3) // 限制数量避免上下文过长
//                .collect(Collectors.joining("\n---\n"));
//    }
//
//    /**
//     * 判断是否为编码规范文档
//     *
//     * @param document 文档
//     * @return true如果是编码规范文档
//     */
//    private boolean isStyleGuideDocument(Document document) {
//        Map<String, Object> metadata = document.getMetadata();
//        String source = (String) metadata.get("source");
//        String type = (String) metadata.get("type");
//
//        return "style_guide".equals(type) ||
//                (source != null && source.contains("style_guide"));
//    }
//
//    /**
//     * 判断是否为指定项目的文档
//     *
//     * @param document     文档
//     * @param repoFullName 仓库全名
//     * @return true如果是指定项目的文档
//     */
//    private boolean isProjectDocument(Document document, String repoFullName) {
//        Map<String, Object> metadata = document.getMetadata();
//        String source = (String) metadata.get("source");
//        String repo = (String) metadata.get("repository");
//        String type = (String) metadata.get("type");
//
//        return "project_code".equals(type) &&
//                (repoFullName.equals(repo) ||
//                        (source != null && source.contains(repoFullName)));
//    }
//}