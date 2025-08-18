package com.way.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.way.config.ElasticsearchConfig;
import com.way.model.dto.SearchResultDTO;
import com.way.model.es.CodeChunk;
import com.way.model.es.DocumentIndex;
import com.way.service.VectorEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版混合检索服务
 * 实现真正的BM25 TopK + kNN TopK混合检索，支持RRF融合
 * 
 * @author way
 */
@Service
public class EnhancedHybridSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedHybridSearchService.class);
    private static final int DEFAULT_TOP_K = 10;
    private static final double RRF_CONSTANT = 60.0; // RRF融合常数
    
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchConfig.ElasticsearchProperties esProperties;
    private final VectorEmbeddingService vectorEmbeddingService;

    @Autowired
    public EnhancedHybridSearchService(ElasticsearchClient elasticsearchClient,
                                     ElasticsearchConfig.ElasticsearchProperties esProperties,
                                     VectorEmbeddingService vectorEmbeddingService) {
        this.elasticsearchClient = elasticsearchClient;
        this.esProperties = esProperties;
        this.vectorEmbeddingService = vectorEmbeddingService;
    }

    /**
     * 混合检索：BM25 TopK + kNN TopK + RRF融合
     * @param query 查询文本
     * @param taskId 任务ID，可选过滤条件
     * @param topK TopK数量，默认10
     * @return 融合后的检索结果列表
     */
    public List<SearchResultDTO> hybridSearch(String query, String taskId, Integer topK) {
        if (!StringUtils.hasText(query)) {
            logger.warn("查询文本为空，返回空结果");
            return new ArrayList<>();
        }

        int k = topK != null ? topK : DEFAULT_TOP_K;
        logger.info("开始增强混合检索: query={}, taskId={}, topK={}", query, taskId, k);

        try {
            // 生成查询向量
            float[] queryVector = vectorEmbeddingService.generateEmbedding(query);
            if (queryVector == null) {
                logger.warn("无法生成查询向量，降级为纯文本检索");
                return textOnlySearch(query, taskId, k);
            }

            // 1. 获取代码块的BM25和kNN结果
            List<SearchResultDTO> codeBM25Results = bm25SearchCodeChunks(query, taskId, k);
            List<SearchResultDTO> codeKnnResults = knnSearchCodeChunks(queryVector, taskId, k);
            
            // 2. 获取文档的BM25和kNN结果
            List<SearchResultDTO> docBM25Results = bm25SearchDocuments(query, taskId, k);
            List<SearchResultDTO> docKnnResults = knnSearchDocuments(queryVector, taskId, k);

            // 3. 分别对代码和文档结果进行RRF融合
            List<SearchResultDTO> fusedCodeResults = applyRRFFusion(codeBM25Results, codeKnnResults, k);
            List<SearchResultDTO> fusedDocResults = applyRRFFusion(docBM25Results, docKnnResults, k);

            // 4. 合并所有结果并最终排序
            List<SearchResultDTO> finalResults = new ArrayList<>();
            finalResults.addAll(fusedCodeResults);
            finalResults.addAll(fusedDocResults);

            // 5. 按分数排序并限制数量
            finalResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            if (finalResults.size() > k) {
                finalResults = finalResults.subList(0, k);
            }

            logger.info("增强混合检索完成: 返回{}个结果, BM25代码{}个, kNN代码{}个, BM25文档{}个, kNN文档{}个", 
                    finalResults.size(), codeBM25Results.size(), codeKnnResults.size(), 
                    docBM25Results.size(), docKnnResults.size());
            
            return finalResults;

        } catch (Exception e) {
            logger.error("增强混合检索失败: query={}, error={}", query, e.getMessage(), e);
            return textOnlySearch(query, taskId, k);
        }
    }

    /**
     * 应用RRF (Reciprocal Rank Fusion) 融合算法
     */
    private List<SearchResultDTO> applyRRFFusion(List<SearchResultDTO> bm25Results, 
                                                List<SearchResultDTO> knnResults, 
                                                int topK) {
        Map<String, SearchResultDTO> resultMap = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 处理BM25结果
        for (int i = 0; i < bm25Results.size(); i++) {
            SearchResultDTO result = bm25Results.get(i);
            String key = generateResultKey(result);
            resultMap.put(key, result);
            
            // RRF分数: 1 / (rank + constant)
            double rrfScore = 1.0 / (i + 1 + RRF_CONSTANT);
            rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + rrfScore);
        }

        // 处理kNN结果
        for (int i = 0; i < knnResults.size(); i++) {
            SearchResultDTO result = knnResults.get(i);
            String key = generateResultKey(result);
            
            if (!resultMap.containsKey(key)) {
                resultMap.put(key, result);
            }
            
            double rrfScore = 1.0 / (i + 1 + RRF_CONSTANT);
            rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + rrfScore);
        }

        // 更新分数并排序
        List<SearchResultDTO> fusedResults = resultMap.values().stream()
                .peek(result -> {
                    String key = generateResultKey(result);
                    result.setScore(rrfScores.get(key));
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());

        logger.info("RRF融合完成: BM25 {}个, kNN {}个, 融合后 {}个", 
                bm25Results.size(), knnResults.size(), fusedResults.size());
        
        return fusedResults;
    }

    /**
     * 生成结果的唯一键，用于去重
     */
    private String generateResultKey(SearchResultDTO result) {
        if ("code".equals(result.getType())) {
            return String.format("code:%s:%s:%s", 
                    result.getTaskId(), result.getClassName(), result.getMethodName());
        } else {
            return String.format("doc:%s:%s", 
                    result.getTaskId(), result.getTitle());
        }
    }

    /**
     * BM25检索代码块
     */
    private List<SearchResultDTO> bm25SearchCodeChunks(String query, String taskId, int size) throws Exception {
        List<Query> mustQueries = new ArrayList<>();
        
        // 添加多字段文本匹配查询
        mustQueries.add(MultiMatchQuery.of(m -> m
            .query(query)
            .fields("content^2.0", "apiName^1.8", "docSummary^1.5", "className^1.2", "methodName^1.2")
            .type(TextQueryType.BestFields)
            .operator(Operator.Or)
            .minimumShouldMatch("75%")
        )._toQuery());
        
        // 添加任务过滤
        if (StringUtils.hasText(taskId)) {
            mustQueries.add(TermQuery.of(t -> t
                .field("taskId")
                .value(taskId)
            )._toQuery());
        }

        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(esProperties.getIndices().getCodeChunks())
            .query(BoolQuery.of(b -> b.must(mustQueries))._toQuery())
            .size(size)
        );

        SearchResponse<CodeChunk> response = elasticsearchClient.search(searchRequest, CodeChunk.class);
        List<SearchResultDTO> results = convertCodeChunkHits(response.hits());
        
        logger.info("BM25代码块检索完成: 查询={}, 结果数={}", query, results.size());
        return results;
    }

    /**
     * kNN检索代码块
     */
    private List<SearchResultDTO> knnSearchCodeChunks(float[] queryVector, String taskId, int size) throws Exception {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(esProperties.getIndices().getCodeChunks())
            .size(size);

        // 添加kNN查询
        List<Float> vectorList = new ArrayList<>();
        for (float value : queryVector) {
            vectorList.add(value);
        }
        
        searchBuilder.knn(knn -> knn
            .field("vector")
            .queryVector(vectorList)
            .k(size * 2)
            .numCandidates(size * 10)
        );

        // 添加任务过滤
        if (StringUtils.hasText(taskId)) {
            searchBuilder.query(TermQuery.of(t -> t
                .field("taskId")
                .value(taskId)
            )._toQuery());
        }

        SearchRequest searchRequest = searchBuilder.build();
        SearchResponse<CodeChunk> response = elasticsearchClient.search(searchRequest, CodeChunk.class);
        List<SearchResultDTO> results = convertCodeChunkHits(response.hits());
        
        logger.info("kNN代码块检索完成: 结果数={}", results.size());
        return results;
    }

    /**
     * BM25检索文档
     */
    private List<SearchResultDTO> bm25SearchDocuments(String query, String taskId, int size) throws Exception {
        List<Query> mustQueries = new ArrayList<>();
        
        // 添加文本匹配查询
        mustQueries.add(MultiMatchQuery.of(m -> m
            .query(query)
            .fields("content^2.0", "name^1.5")
            .type(TextQueryType.BestFields)
            .operator(Operator.Or)
            .minimumShouldMatch("75%")
        )._toQuery());
        
        // 添加任务过滤
        if (StringUtils.hasText(taskId)) {
            mustQueries.add(TermQuery.of(t -> t
                .field("taskId")
                .value(taskId)
            )._toQuery());
        }

        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(esProperties.getIndices().getDocuments())
            .query(BoolQuery.of(b -> b.must(mustQueries))._toQuery())
            .size(size)
        );

        SearchResponse<DocumentIndex> response = elasticsearchClient.search(searchRequest, DocumentIndex.class);
        List<SearchResultDTO> results = convertDocumentHits(response.hits());
        
        logger.info("BM25文档检索完成: 查询={}, 结果数={}", query, results.size());
        return results;
    }

    /**
     * kNN检索文档
     */
    private List<SearchResultDTO> knnSearchDocuments(float[] queryVector, String taskId, int size) throws Exception {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(esProperties.getIndices().getDocuments())
            .size(size);

        // 添加kNN查询
        List<Float> vectorList = new ArrayList<>();
        for (float value : queryVector) {
            vectorList.add(value);
        }
        
        searchBuilder.knn(knn -> knn
            .field("vector")
            .queryVector(vectorList)
            .k(size * 2)
            .numCandidates(size * 10)
        );

        // 添加任务过滤
        if (StringUtils.hasText(taskId)) {
            searchBuilder.query(TermQuery.of(t -> t
                .field("taskId")
                .value(taskId)
            )._toQuery());
        }

        SearchRequest searchRequest = searchBuilder.build();
        SearchResponse<DocumentIndex> response = elasticsearchClient.search(searchRequest, DocumentIndex.class);
        List<SearchResultDTO> results = convertDocumentHits(response.hits());
        
        logger.info("kNN文档检索完成: 结果数={}", results.size());
        return results;
    }

    /**
     * 纯文本检索（降级方案）
     */
    private List<SearchResultDTO> textOnlySearch(String query, String taskId, int size) {
        List<SearchResultDTO> results = new ArrayList<>();
        
        try {
            // 检索代码块和文档
            results.addAll(bm25SearchCodeChunks(query, taskId, size / 2));
            results.addAll(bm25SearchDocuments(query, taskId, size / 2));
            
            // 按分数排序并限制数量
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            if (results.size() > size) {
                results = results.subList(0, size);
            }
            
            logger.info("降级为纯文本检索完成: 返回{}个结果", results.size());
            
        } catch (Exception e) {
            logger.error("纯文本检索失败: query={}, error={}", query, e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * 转换代码块检索结果
     */
    private List<SearchResultDTO> convertCodeChunkHits(HitsMetadata<CodeChunk> hits) {
        List<SearchResultDTO> results = new ArrayList<>();
        
        for (Hit<CodeChunk> hit : hits.hits()) {
            CodeChunk chunk = hit.source();
            if (chunk != null) {
                SearchResultDTO result = new SearchResultDTO();
                result.setId(hit.id());
                result.setScore(hit.score());
                result.setType("code");
                result.setTaskId(chunk.getTaskId()); // 使用taskId
                result.setRepoId(chunk.getRepoId()); // 保持向后兼容
                result.setContent(chunk.getContent());
                result.setSummary(chunk.getDocSummary());
                result.setLanguage(chunk.getLanguage());
                result.setClassName(chunk.getClassName());
                result.setMethodName(chunk.getMethodName());
                result.setApiName(chunk.getApiName());
                result.setTitle(chunk.getApiName() != null ? chunk.getApiName() : 
                              (chunk.getClassName() + "#" + chunk.getMethodName()));
                
                results.add(result);
            }
        }
        
        return results;
    }

    /**
     * 转换文档检索结果
     */
    private List<SearchResultDTO> convertDocumentHits(HitsMetadata<DocumentIndex> hits) {
        List<SearchResultDTO> results = new ArrayList<>();
        
        for (Hit<DocumentIndex> hit : hits.hits()) {
            DocumentIndex doc = hit.source();
            if (doc != null) {
                SearchResultDTO result = new SearchResultDTO();
                result.setId(hit.id());
                result.setScore(hit.score());
                result.setType("document");
                result.setTaskId(doc.getTaskId()); // 使用taskId
                result.setRepoId(doc.getRepoId()); // 保持向后兼容
                result.setTitle(doc.getName());
                result.setContent(doc.getContent());
                result.setSummary(truncateContent(doc.getContent(), 200));
                
                results.add(result);
            }
        }
        
        return results;
    }

    /**
     * 截断内容用作摘要
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 检查服务是否可用
     */
    public boolean isServiceAvailable() {
        try {
            elasticsearchClient.ping();
            return vectorEmbeddingService.isServiceAvailable();
        } catch (Exception e) {
            logger.info("增强混合检索服务不可用: {}", e.getMessage());
            return false;
        }
    }
}
