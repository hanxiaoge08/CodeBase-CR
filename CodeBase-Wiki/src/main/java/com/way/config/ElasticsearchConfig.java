package com.way.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Elasticsearch配置类
 *
 * @author way
 */
@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchProperties properties) {
        // 创建认证凭据
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));

        // 解析ES URI
        String[] uriParts = properties.getUris().replace("http://", "").split(":");
        String hostname = uriParts[0];
        int port = Integer.parseInt(uriParts[1]);

        // 创建RestClient
        RestClient restClient = RestClient.builder(
                        new HttpHost(hostname, port, "http"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setConnectionTimeToLive(Duration.ofMinutes(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS))
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(properties.getConnectionTimeout())
                                .setSocketTimeout(properties.getSocketTimeout()))
                .build();

        // 配置Jackson ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 创建传输层
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(objectMapper));

        return new ElasticsearchClient(transport);
    }

    @Component
    @ConfigurationProperties(prefix = "elasticsearch")
    public static class ElasticsearchProperties {
        private String uris;
        private String username;
        private String password;
        private int connectionTimeout = 5000;
        private int socketTimeout = 60000;
        private Indices indices = new Indices();
        private CodeParser codeParser = new CodeParser();

        // Getters and setters
        public String getUris() {
            return uris;
        }

        public void setUris(String uris) {
            this.uris = uris;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public Indices getIndices() {
            return indices;
        }

        public void setIndices(Indices indices) {
            this.indices = indices;
        }

        public CodeParser getCodeParser() {
            return codeParser;
        }

        public void setCodeParser(CodeParser codeParser) {
            this.codeParser = codeParser;
        }

        public static class Indices {
            private String codeChunks = "code_chunks_index";
            private String documents = "documents_index";

            public String getCodeChunks() {
                return codeChunks;
            }

            public void setCodeChunks(String codeChunks) {
                this.codeChunks = codeChunks;
            }

            public String getDocuments() {
                return documents;
            }

            public void setDocuments(String documents) {
                this.documents = documents;
            }
        }

        public static class CodeParser {
            private String url;
            private int timeout;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public int getTimeout() {
                return timeout;
            }

            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }
        }
    }
}
