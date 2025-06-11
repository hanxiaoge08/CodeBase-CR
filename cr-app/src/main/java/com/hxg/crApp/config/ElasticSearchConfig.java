//package com.hxg.crApp.config;
//
//import org.apache.http.HttpHost;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.elasticsearch.client.RestClient;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.embedding.TokenCountBatchingStrategy;
//import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
//import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
//import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * @author hxg
// * @description: ES配置类
// * @date 2025/6/11 09:34
// */
//public class ElasticSearchConfig {
//
//    private static final Logger logger= LoggerFactory.getLogger(ElasticSearchConfig.class);
//
//    @Value("${spring.elasticsearch.uris}")
//    public String url;
//
//    @Value("${spring.elasticsearch.username}")
//    private String username;
//    @Value("${spring.elasticsearch.password}")
//    private String password;
//
//    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
//    private String indexName;
//    @Value("${spring.ai.vectorstore.elasticsearch.similarity}")
//    private SimilarityFunction similarityFunction;
//    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
//    private int dimensions;
//
//    /**
//     * 创建ElasticSearch REST客户端Bean
//     * 该方法解析配置的URL地址，构建带认证的RestClient实例
//     *
//     * @return 配置好的RestClient实例
//     */
//    @Bean
//    public RestClient restClient(){
//        /*
//         * 解析URL为协议、主机和端口部分
//         * 支持标准的协议://host:port格式
//         */
//        String[] urlParts=url.split("://");
//        String protocol=urlParts[0];
//        String hostAndPort=urlParts[1];
//        String[] hostAndPorts=hostAndPort.split(":");
//        String host=hostAndPorts[0];
//        int port=Integer.parseInt(hostAndPorts[1]);
//
//        /*
//         * 创建基础认证凭证提供者
//         * 使用配置的用户名密码进行身份验证
//         */
//        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY,
//                new UsernamePasswordCredentials(username,password));
//
//        logger.info("create elasticsearch rest client");
//        /*
//         * 构建RestClient实例
//         * 配置HTTP客户端的凭证提供者
//         * 返回构建好的客户端对象
//         */
//        return RestClient.builder(new HttpHost(host, port, protocol))
//                .setHttpClientConfigCallback(httpClientBuilder -> {
//                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//                    return httpClientBuilder;
//                })
//                .build();
//    }
//
//    @Bean
//    @Qualifier("elasticsearchVectorStore")
//    public ElasticsearchVectorStore vectorStore(RestClient restClient,EmbeddingModel embeddingModel){
//        logger.info("create elasticsearch vector store");
//
//        /*
//         * 创建并配置ElasticsearchVectorStoreOptions：
//         * - 索引名称：indexName
//         * - 相似度函数：similarityFunction
//         * - 向量维度：dimensions
//         */
//        ElasticsearchVectorStoreOptions options=new ElasticsearchVectorStoreOptions();
//        options.setIndexName(indexName);
//        options.setSimilarity(similarityFunction);
//        options.setDimensions(dimensions);
//
//        /*
//         * 构建向量存储实例时配置：
//         * - 自动初始化schema
//         * - 使用TokenCountBatchingStrategy批处理策略
//         */
//        return ElasticsearchVectorStore.builder(restClient,embeddingModel)
//                .options(options)
//                .initializeSchema(true)
//                .batchingStrategy(new TokenCountBatchingStrategy())
//                .build();
//    }
//
//}
