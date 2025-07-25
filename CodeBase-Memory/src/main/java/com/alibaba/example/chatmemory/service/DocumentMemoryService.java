package com.alibaba.example.chatmemory.service;

import com.alibaba.example.chatmemory.mem0.MemZeroServerRequest;
import com.alibaba.example.chatmemory.mem0.MemZeroServerResp;
import com.alibaba.example.chatmemory.mem0.MemZeroServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文档记忆服务
 * 专门处理Wiki模块生成的文档内容存储到Mem0
 * 
 * @author AI Assistant
 */
@Service
public class DocumentMemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentMemoryService.class);
    
    @Autowired
    private MemZeroServiceClient memZeroServiceClient;
    
    /**
     * 异步添加文档内容到记忆
     * 
     * @param repositoryId 仓库ID
     * @param documentName 文档名称
     * @param documentContent 文档内容
     * @param documentUrl 文档URL路径
     * @param metadata 额外的元数据
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> addDocumentMemoryAsync(String repositoryId, 
                                                         String documentName,
                                                         String documentContent, 
                                                         String documentUrl,
                                                         Map<String, Object> metadata) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始添加文档记忆: repositoryId={}, documentName={}", repositoryId, documentName);
                
                // 构建记忆消息
                List<MemZeroServerRequest.Message> messages = List.of(
                    MemZeroServerRequest.Message.Builder.builder()
                        .role("system")
                        .content("这是一个代码仓库的文档内容，用于理解项目结构和功能。")
                        .build(),
                    MemZeroServerRequest.Message.Builder.builder()
                        .role("user")
                        .content(String.format("文档名称: %s\n文档路径: %s\n\n文档内容:\n%s", 
                            documentName, documentUrl, documentContent))
                        .build()
                );
                
                // 构建元数据
                Map<String, Object> enrichedMetadata = Map.of(
                    "type", "document",
                    "repository_id", repositoryId,
                    "document_name", documentName,
                    "document_url", documentUrl,
                    "content_type", "wiki_document",
                    "timestamp", System.currentTimeMillis()
                );
                
                // 如果有额外元数据，合并
                if (metadata != null && !metadata.isEmpty()) {
                    enrichedMetadata = new java.util.HashMap<>(enrichedMetadata);
                    enrichedMetadata.putAll(metadata);
                }
                
                // 创建记忆请求
                MemZeroServerRequest.MemoryCreate memoryCreate = MemZeroServerRequest.MemoryCreate.builder()
                    .messages(messages)
                    .userId(repositoryId)  // 使用仓库ID作为用户ID
                    .metadata(enrichedMetadata)
                    .build();
                
                // 添加到Mem0
                memZeroServiceClient.addMemory(memoryCreate);
                
                logger.info("成功添加文档记忆: repositoryId={}, documentName={}", repositoryId, documentName);
                
            } catch (Exception e) {
                logger.error("添加文档记忆失败: repositoryId={}, documentName={}", repositoryId, documentName, e);
                throw new RuntimeException("添加文档记忆失败", e);
            }
        });
    }
    
    /**
     * 异步添加代码文件内容到记忆
     * 
     * @param repositoryId 仓库ID
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param fileContent 文件内容
     * @param fileType 文件类型
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> addCodeFileMemoryAsync(String repositoryId,
                                                         String fileName,
                                                         String filePath,
                                                         String fileContent,
                                                         String fileType) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始添加代码文件记忆: repositoryId={}, fileName={}", repositoryId, fileName);
                
                // 构建记忆消息
                List<MemZeroServerRequest.Message> messages = List.of(
                    MemZeroServerRequest.Message.Builder.builder()
                        .role("system")
                        .content("这是一个代码仓库的源代码文件，用于理解项目实现和逻辑。")
                        .build(),
                    MemZeroServerRequest.Message.Builder.builder()
                        .role("user")
                        .content(String.format("文件名: %s\n文件路径: %s\n文件类型: %s\n\n代码内容:\n```%s\n%s\n```", 
                            fileName, filePath, fileType, getLanguageFromFileType(fileType), fileContent))
                        .build()
                );
                
                // 构建元数据
                Map<String, Object> metadata = Map.of(
                    "type", "code_file",
                    "repository_id", repositoryId,
                    "file_name", fileName,
                    "file_path", filePath,
                    "file_type", fileType,
                    "content_type", "source_code",
                    "timestamp", System.currentTimeMillis()
                );
                
                // 创建记忆请求
                MemZeroServerRequest.MemoryCreate memoryCreate = MemZeroServerRequest.MemoryCreate.builder()
                    .messages(messages)
                    .userId(repositoryId)  // 使用仓库ID作为用户ID
                    .metadata(metadata)
                    .build();
                
                // 添加到Mem0
                memZeroServiceClient.addMemory(memoryCreate);
                
                logger.info("成功添加代码文件记忆: repositoryId={}, fileName={}", repositoryId, fileName);
                
            } catch (Exception e) {
                logger.error("添加代码文件记忆失败: repositoryId={}, fileName={}", repositoryId, fileName, e);
                throw new RuntimeException("添加代码文件记忆失败", e);
            }
        });
    }
    
    /**
     * 批量添加文档记忆
     * 
     * @param repositoryId 仓库ID
     * @param documents 文档列表
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> batchAddDocumentMemoriesAsync(String repositoryId,
                                                                List<DocumentInfo> documents) {
        
        return CompletableFuture.runAsync(() -> {
            logger.info("开始批量添加文档记忆: repositoryId={}, documentCount={}", repositoryId, documents.size());
            
            // 并行处理所有文档
            List<CompletableFuture<Void>> futures = documents.stream()
                .map(doc -> addDocumentMemoryAsync(repositoryId, doc.getName(), doc.getContent(), 
                    doc.getUrl(), doc.getMetadata()))
                .toList();
            
            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            logger.info("批量添加文档记忆完成: repositoryId={}, documentCount={}", repositoryId, documents.size());
        });
    }
    
    /**
     * 根据文件类型获取编程语言
     */
    private String getLanguageFromFileType(String fileType) {
        if (fileType == null) return "";
        
        return switch (fileType.toLowerCase()) {
            case "java" -> "java";
            case "js", "javascript" -> "javascript";
            case "ts", "typescript" -> "typescript";
            case "py", "python" -> "python";
            case "cpp", "c++" -> "cpp";
            case "c" -> "c";
            case "go" -> "go";
            case "rs", "rust" -> "rust";
            case "php" -> "php";
            case "rb", "ruby" -> "ruby";
            case "swift" -> "swift";
            case "kt", "kotlin" -> "kotlin";
            case "scala" -> "scala";
            case "cs", "csharp" -> "csharp";
            case "xml" -> "xml";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "sql" -> "sql";
            case "sh", "bash" -> "bash";
            default -> "";
        };
    }
    
    /**
     * 文档信息内部类
     */
    public static class DocumentInfo {
        private String name;
        private String content;
        private String url;
        private Map<String, Object> metadata;
        
        public DocumentInfo(String name, String content, String url) {
            this.name = name;
            this.content = content;
            this.url = url;
        }
        
        public DocumentInfo(String name, String content, String url, Map<String, Object> metadata) {
            this.name = name;
            this.content = content;
            this.url = url;
            this.metadata = metadata;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
} 