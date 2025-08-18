package com.way.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;

import java.io.StringReader;
import com.way.config.ElasticsearchConfig;
import com.way.model.es.CodeChunk;
import com.way.model.es.DocumentIndex;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Elasticsearch索引服务
 * 处理代码块和文档的索引操作
 * @author way
 */
@Service
public class ElasticsearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexService.class);

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchConfig.ElasticsearchProperties esProperties;
    private final VectorEmbeddingService vectorEmbeddingService;

    @Autowired
    public ElasticsearchIndexService(ElasticsearchClient elasticsearchClient,
                                     ElasticsearchConfig.ElasticsearchProperties esProperties,
                                     VectorEmbeddingService vectorEmbeddingService) {
        this.elasticsearchClient = elasticsearchClient;
        this.esProperties = esProperties;
        this.vectorEmbeddingService = vectorEmbeddingService;

        // 初始化索引
        initializeIndices();
    }

    /**
     * 初始化ES索引
     */
    private void initializeIndices() {
        try {
            createCodeChunksIndexIfNotExists();
            createDocumentsIndexIfNotExists();
            logger.info("ES索引初始化完成");
        } catch (Exception e) {
            logger.error("ES索引初始化失败", e);
        }
    }

    /**
     * 创建代码块索引
     */
    private void createCodeChunksIndexIfNotExists() throws Exception {
        String indexName = esProperties.getIndices().getCodeChunks();

        if (!indexExists(indexName)) {
            logger.info("创建代码块索引: {}", indexName);

            IndexSettings settings = IndexSettings.of(s -> s
                    .numberOfShards("3")
                    .numberOfReplicas("1")
                    .analysis(a -> a.withJson(new StringReader("""
                    {
                        "analyzer": {
                            "code_ana": {
                                "type": "custom",
                                "tokenizer": "pattern",
                                "filter": ["lowercase", "kstem"]
                            },
                            "zh_ik": {
                                "type": "custom", 
                                "tokenizer": "ik_max_word",
                                "filter": ["lowercase"]
                            }
                        }
                    }
                    """)))
            );

            // 创建映射
            Map<String, Property> mappings = new HashMap<>();
            mappings.put("repoId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("taskId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("language", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("className", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("methodName", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("apiName", Property.of(p -> p.text(TextProperty.of(t -> t
                    .analyzer("code_ana")
                    .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
            ))));
            mappings.put("docSummary", Property.of(p -> p.text(TextProperty.of(t -> t.analyzer("zh_ik")))));
            mappings.put("content", Property.of(p -> p.text(TextProperty.of(t -> t
                    .analyzer("code_ana")
                    .termVector(TermVectorOption.WithPositionsOffsets)
            ))));
            mappings.put("sha256", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("mtime", Property.of(p -> p.date(d -> d)));
            mappings.put("chunk_size", Property.of(p -> p.integer(i -> i)));
            mappings.put("vector", Property.of(p -> p.denseVector(DenseVectorProperty.of(d -> d
                    .dims(1024)
                    .index(true)
                    .similarity("cosine")
            ))));

            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .settings(settings)
                    .mappings(m -> m
                            .dynamic(DynamicMapping.False)
                            .properties(mappings)
                    )
            );

            elasticsearchClient.indices().create(request);
            logger.info("代码块索引创建成功: {}", indexName);
        }
    }

    /**
     * 创建文档索引
     */
    private void createDocumentsIndexIfNotExists() throws Exception {
        String indexName = esProperties.getIndices().getDocuments();

        if (!indexExists(indexName)) {
            logger.info("创建文档索引: {}", indexName);

            IndexSettings settings = IndexSettings.of(s -> s
                    .numberOfShards("3")
                    .numberOfReplicas("1")
                    .analysis(a -> a.withJson(new StringReader("""
                    {
                        "analyzer": {
                            "zh_ik": {
                                "type": "custom",
                                "tokenizer": "ik_max_word", 
                                "filter": ["lowercase"]
                            }
                        }
                    }
                    """)))
            );

            // 创建映射
            Map<String, Property> mappings = new HashMap<>();
            mappings.put("repoId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("taskId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("catalogueId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("name", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("content", Property.of(p -> p.text(TextProperty.of(t -> t
                    .analyzer("zh_ik")
                    .termVector(TermVectorOption.WithPositionsOffsets)
            ))));
            mappings.put("sha256", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
            mappings.put("mtime", Property.of(p -> p.date(d -> d)));
            mappings.put("status", Property.of(p -> p.integer(i -> i)));
            mappings.put("vector", Property.of(p -> p.denseVector(DenseVectorProperty.of(d -> d
                    .dims(1024)
                    .index(true)
                    .similarity("cosine")
            ))));

            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .settings(settings)
                    .mappings(m -> m
                            .dynamic(DynamicMapping.False)
                            .properties(mappings)
                    )
            );

            elasticsearchClient.indices().create(request);
            logger.info("文档索引创建成功: {}", indexName);
        }
    }

    /**
     * 检查索引是否存在
     */
    private boolean indexExists(String indexName) throws Exception {
        ExistsRequest request = ExistsRequest.of(e -> e.index(indexName));
        return elasticsearchClient.indices().exists(request).value();
    }

    /**
     * 索引代码块
     * @param codeChunk 代码块
     * @return 索引结果
     */
    public CompletableFuture<Boolean> indexCodeChunk(CodeChunk codeChunk) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 生成文档ID
                String docId = generateCodeChunkId(codeChunk);

                // 计算内容哈希
                if (codeChunk.getContent() != null) {
                    codeChunk.setSha256(DigestUtils.sha256Hex(codeChunk.getContent()));
                }

                // 设置时间戳
                if (codeChunk.getMtime() == null) {
                    codeChunk.setMtime(LocalDateTime.now());
                }

                // 生成向量嵌入
                if (vectorEmbeddingService.isServiceAvailable()) {
                    try {
                        float[] vector = vectorEmbeddingService.generateCodeEmbedding(
                                codeChunk.getClassName(),
                                codeChunk.getMethodName(),
                                codeChunk.getDocSummary(),
                                codeChunk.getContent()
                        );

                        if (vector != null) {
                            codeChunk.setVector(vector);
                            logger.debug("代码块向量嵌入生成成功: docId={}, vectorDimension={}",
                                    docId, vector.length);
                        } else {
                            logger.warn("代码块向量嵌入生成失败: docId={}", docId);
                        }
                    } catch (Exception vectorError) {
                        logger.error("生成代码块向量嵌入时发生异常: docId={}, error={}",
                                docId, vectorError.getMessage());
                    }
                } else {
                    logger.debug("向量嵌入服务不可用，跳过向量生成: docId={}", docId);
                }

                IndexRequest<CodeChunk> request = IndexRequest.of(i -> i
                        .index(esProperties.getIndices().getCodeChunks())
                        .id(docId)
                        .document(codeChunk)
                );

                IndexResponse response = elasticsearchClient.index(request);

                logger.debug("代码块索引成功: docId={}, result={}, hasVector={}",
                        docId, response.result(), codeChunk.getVector() != null);
                return true;

            } catch (Exception e) {
                logger.error("代码块索引失败: repoId={}, apiName={}, error={}",
                        codeChunk.getRepoId(), codeChunk.getApiName(), e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 索引文档
     * @param documentIndex 文档索引
     * @return 索引结果
     */
    public CompletableFuture<Boolean> indexDocument(DocumentIndex documentIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 生成文档ID
                String docId = generateDocumentId(documentIndex);

                // 计算内容哈希
                if (documentIndex.getContent() != null) {
                    documentIndex.setSha256(DigestUtils.sha256Hex(documentIndex.getContent()));
                }

                // 设置时间戳
                if (documentIndex.getMtime() == null) {
                    documentIndex.setMtime(LocalDateTime.now());
                }

                // 生成向量嵌入
                if (vectorEmbeddingService.isServiceAvailable()) {
                    try {
                        float[] vector = vectorEmbeddingService.generateDocumentEmbedding(
                                documentIndex.getName(),
                                documentIndex.getContent()
                        );

                        if (vector != null) {
                            documentIndex.setVector(vector);
                            logger.debug("文档向量嵌入生成成功: docId={}, vectorDimension={}",
                                    docId, vector.length);
                        } else {
                            logger.warn("文档向量嵌入生成失败: docId={}", docId);
                        }
                    } catch (Exception vectorError) {
                        logger.error("生成文档向量嵌入时发生异常: docId={}, error={}",
                                docId, vectorError.getMessage());
                    }
                } else {
                    logger.debug("向量嵌入服务不可用，跳过向量生成: docId={}", docId);
                }

                IndexRequest<DocumentIndex> request = IndexRequest.of(i -> i
                        .index(esProperties.getIndices().getDocuments())
                        .id(docId)
                        .document(documentIndex)
                );

                IndexResponse response = elasticsearchClient.index(request);

                logger.debug("文档索引成功: docId={}, result={}, hasVector={}",
                        docId, response.result(), documentIndex.getVector() != null);
                return true;

            } catch (Exception e) {
                logger.error("文档索引失败: repoId={}, catalogueId={}, error={}",
                        documentIndex.getRepoId(), documentIndex.getCatalogueId(), e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 生成代码块文档ID
     */
    private String generateCodeChunkId(CodeChunk codeChunk) {
        String combined = codeChunk.getRepoId() + ":" +
                (codeChunk.getApiName() != null ? codeChunk.getApiName() : codeChunk.getClassName() + ":" + codeChunk.getMethodName());
        return DigestUtils.sha256Hex(combined);
    }

    /**
     * 生成文档ID  
     */
    private String generateDocumentId(DocumentIndex documentIndex) {
        String combined = documentIndex.getRepoId() + ":" + documentIndex.getCatalogueId();
        return DigestUtils.sha256Hex(combined);
    }

    /**
     * 检查ES服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            elasticsearchClient.ping();
            return true;
        } catch (Exception e) {
            logger.debug("ES服务不可用: {}", e.getMessage());
            return false;
        }
    }
}
