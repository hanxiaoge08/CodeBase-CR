package com.way.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.way.config.ElasticsearchConfig;
import com.way.model.dto.CodeParseRequest;
import com.way.model.dto.CodeParseResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

/**
 * 代码解析服务
 * 调用AST解析接口获取代码块
 * @author way
 */
@Service
public class CodeParseService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeParseService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ElasticsearchConfig.ElasticsearchProperties esProperties;

    @Autowired
    public CodeParseService(ElasticsearchConfig.ElasticsearchProperties esProperties) {
        this.esProperties = esProperties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(esProperties.getCodeParser().getTimeout()))
                .readTimeout(Duration.ofMillis(esProperties.getCodeParser().getTimeout()))
                .writeTimeout(Duration.ofMillis(esProperties.getCodeParser().getTimeout()))
                .build();
    }

    /**
     * 解析代码文件获取代码块
     * @param language 编程语言
     * @param code 代码内容
     * @return 解析后的代码块列表
     */
    public CodeParseResponse parseCode(String language, String code) {
        return parseCode(language, code, 1000);
    }

    /**
     * 解析代码文件获取代码块
     * @param language 编程语言
     * @param code 代码内容
     * @param maxChars 最大字符数
     * @return 解析后的代码块列表
     */
    public CodeParseResponse parseCode(String language, String code, Integer maxChars) {
        try {
            // 构建请求体
            CodeParseRequest request = new CodeParseRequest(language, code, maxChars);
            String requestJson = objectMapper.writeValueAsString(request);
            
            RequestBody body = RequestBody.create(requestJson, JSON);
            Request httpRequest = new Request.Builder()
                    .url(esProperties.getCodeParser().getUrl() + "/parse")
                    .post(body)
                    .build();

            logger.debug("发送代码解析请求: language={}, codeLength={}, maxChars={}", 
                    language, code.length(), maxChars);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("代码解析请求失败: code={}, message={}", response.code(), response.message());
                    return createEmptyResponse();
                }

                String responseBody = response.body().string();
                CodeParseResponse parseResponse = objectMapper.readValue(responseBody, CodeParseResponse.class);
                
                logger.debug("代码解析成功: language={}, chunksCount={}", 
                        language, parseResponse.getChunks() != null ? parseResponse.getChunks().size() : 0);
                
                return parseResponse;
            }

        } catch (IOException e) {
            logger.error("代码解析失败: language={}, error={}", language, e.getMessage(), e);
            return createEmptyResponse();
        } catch (Exception e) {
            logger.error("代码解析异常: language={}, error={}", language, e.getMessage(), e);
            return createEmptyResponse();
        }
    }

    /**
     * 根据文件扩展名推断编程语言
     * @param fileName 文件名
     * @return 编程语言
     */
    public String inferLanguageFromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "text";
        }
        
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        
        return switch (extension) {
            case "java" -> "java";
            case "js", "jsx" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "py" -> "python";
            case "go" -> "go";
            case "cpp", "cc", "cxx" -> "cpp";
            case "c" -> "c";
            case "h", "hpp" -> "c";
            case "cs" -> "csharp";
            case "php" -> "php";
            case "rb" -> "ruby";
            case "kt" -> "kotlin";
            case "swift" -> "swift";
            case "rs" -> "rust";
            case "scala" -> "scala";
            default -> "text";
        };
    }

    /**
     * 检查代码解析服务是否可用
     * @return 是否可用
     */
    public boolean isServiceAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(esProperties.getCodeParser().getUrl() + "/health")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            logger.debug("代码解析服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建空的响应
     */
    private CodeParseResponse createEmptyResponse() {
        CodeParseResponse response = new CodeParseResponse();
        response.setChunks(Collections.emptyList());
        return response;
    }
}
