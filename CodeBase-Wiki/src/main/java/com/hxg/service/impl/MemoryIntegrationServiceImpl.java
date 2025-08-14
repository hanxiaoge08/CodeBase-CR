package com.hxg.service.impl;

import com.hxg.queue.model.MemoryIndexTask;
import com.hxg.queue.producer.MemoryIndexProducer;
import com.hxg.model.dto.BatchDocumentRequest;
import com.hxg.model.entity.Catalogue;
import com.hxg.service.IMemoryIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 记忆集成服务实现
 * 负责将Wiki生成的文档和代码文件索引到Mem0记忆系统
 * 通过Feign客户端调用Memory模块的REST API
 * 
 * @author AI Assistant
 */
@Service
public class MemoryIntegrationServiceImpl implements IMemoryIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryIntegrationServiceImpl.class);
    
    @Autowired
    private MemoryIndexProducer memoryIndexProducer;
    
    // 支持的代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = Set.of(
        "java", "js", "ts", "py", "go", "cpp", "c", "h", "cs", "php", "rb", "kt", "swift"
    );
    
    // 支持的文档文件扩展名（暂未使用，保留扩展需要时再启用）
    
    @Override
    public CompletableFuture<Void> indexProjectToMemoryAsync(String repositoryId, 
                                                            List<Catalogue> catalogues, 
                                                            String projectPath) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始项目记忆索引: repositoryId={}, catalogueCount={}, projectPath={}", 
                    repositoryId, catalogues.size(), projectPath);
                
                // 索引生成的文档内容
                List<BatchDocumentRequest.DocumentInfo> documents = new ArrayList<>();
                
                for (Catalogue catalogue : catalogues) {
                    if (StringUtils.hasText(catalogue.getContent())) {
                        String documentUrl = generateDocumentUrl(catalogue);
                        Map<String, Object> metadata = createCatalogueMetadata(catalogue, catalogue.getTaskId());
                        
                        BatchDocumentRequest.DocumentInfo docInfo = new BatchDocumentRequest.DocumentInfo(
                            catalogue.getName(),
                            catalogue.getContent(),
                            documentUrl,
                            metadata
                        );
                        documents.add(docInfo);
                    }
                }
                
                // 批量改为逐条发送到Kafka，避免大消息
                if (!documents.isEmpty()) {
                    documents.forEach(doc -> {
                        MemoryIndexTask task = new MemoryIndexTask();
                        task.setType("document");
                        task.setRepositoryId(repositoryId);
                        task.setDocumentName(doc.getName());
                        task.setDocumentContent(doc.getContent());
                        task.setDocumentUrl(doc.getUrl());
                        task.setMetadata(doc.getMetadata());
                        memoryIndexProducer.sendDocumentTask(task);
                    });
                    logger.info("批量文档索引任务已投递到Kafka: repositoryId={}, 文档数量={}", repositoryId, documents.size());
                }
                
                // 并行索引代码文件
                CompletableFuture<Void> codeIndexFuture = indexCodeFilesToMemoryAsync(repositoryId, projectPath);
                
                // 等待代码文件索引完成
                codeIndexFuture.join();
                
                logger.info("项目记忆索引完成: repositoryId={}, 文档数量={}", repositoryId, documents.size());
                
            } catch (Exception e) {
                logger.error("项目记忆索引失败: repositoryId={}", repositoryId, e);
                throw new RuntimeException("项目记忆索引失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> indexDocumentToMemoryAsync(String repositoryId, Catalogue catalogue) {
        
        if (!StringUtils.hasText(catalogue.getContent())) {
            logger.debug("文档内容为空，跳过索引: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("开始单个文档记忆索引: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
                
                String documentUrl = generateDocumentUrl(catalogue);
                Map<String, Object> metadata = createCatalogueMetadata(catalogue, catalogue.getTaskId());
                
                MemoryIndexTask task = new MemoryIndexTask();
                task.setType("document");
                task.setRepositoryId(repositoryId);
                task.setDocumentName(catalogue.getName());
                task.setDocumentContent(catalogue.getContent());
                task.setDocumentUrl(documentUrl);
                task.setMetadata(metadata);
                memoryIndexProducer.sendDocumentTask(task);
                
                logger.debug("单个文档记忆索引完成: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
                
            } catch (Exception e) {
                logger.error("单个文档记忆索引异常: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> indexCodeFilesToMemoryAsync(String repositoryId, String projectPath) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始代码文件记忆索引: repositoryId={}, projectPath={}", repositoryId, projectPath);
                
                Path projectDir = Paths.get(projectPath);
                if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
                    logger.warn("项目路径不存在或不是目录: {}", projectPath);
                    return;
                }
                
                // 扫描代码文件
                List<Path> codeFiles = scanCodeFiles(projectDir);
                logger.info("扫描到代码文件数量: {}", codeFiles.size());
                
                int indexedCount = 0;
                for (Path codeFile : codeFiles) {
                    try {
                        if (indexSingleCodeFile(repositoryId, projectDir, codeFile)) {
                            indexedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("索引代码文件失败: {}", codeFile, e);
                    }
                }
                
                logger.info("代码文件记忆索引完成: repositoryId={}, 总文件数={}, 索引数={}", 
                    repositoryId, codeFiles.size(), indexedCount);
                
            } catch (Exception e) {
                logger.error("代码文件记忆索引异常: repositoryId={}", repositoryId, e);
            }
        });
    }
    
    @Override
    public boolean isMemoryServiceAvailable() {
        // Kafka 模式下，写入由队列承接，不再直连 Memory 服务
        // 这里返回 true，将可靠性交由 Kafka（重试/DLQ）与 Memory 消费端保障
        return true;
    }
    
    /**
     * 扫描项目目录中的代码文件
     */
    private List<Path> scanCodeFiles(Path projectDir) throws IOException {
        List<Path> codeFiles = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(projectDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isCodeFile)
                 .filter(this::shouldIncludeFile)
                 .forEach(codeFiles::add);
        }
        
        return codeFiles;
    }
    
    /**
     * 判断是否为代码文件
     */
    private boolean isCodeFile(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return false;
        
        String extension = fileName.substring(lastDot + 1).toLowerCase();
        return CODE_EXTENSIONS.contains(extension);
    }
    
    /**
     * 判断是否应该包含该文件（排除一些目录和文件）
     */
    private boolean shouldIncludeFile(Path file) {
        String pathStr = file.toString();
        
        // 排除常见的构建目录和隐藏目录
        return !pathStr.contains("target/") &&
               !pathStr.contains("build/") &&
               !pathStr.contains("node_modules/") &&
               !pathStr.contains(".git/") &&
               !pathStr.contains(".idea/") &&
               !pathStr.contains(".vscode/") &&
               !pathStr.contains("/.") &&
               !pathStr.contains("\\.");
    }
    
    /**
     * 索引单个代码文件
     */
    private boolean indexSingleCodeFile(String repositoryId, Path projectDir, Path codeFile) {
        try {
            // 读取文件内容
            String content = Files.readString(codeFile);
            if (content.trim().isEmpty()) {
                return false;
            }
            
            // 计算相对路径
            String relativePath = projectDir.relativize(codeFile).toString().replace('\\', '/');
            String fileName = codeFile.getFileName().toString();
            String fileExtension = getFileExtension(fileName);
            
                    // 构造Kafka任务并发送
                    MemoryIndexTask task = new MemoryIndexTask();
                    task.setType("code_file");
                    task.setRepositoryId(repositoryId);
                    task.setFileName(fileName);
                    task.setFilePath(relativePath);
                    task.setFileContent(content);
                    task.setFileType(fileExtension);
                    memoryIndexProducer.sendCodeFileTask(task);
            
            return true;
            
        } catch (IOException e) {
            logger.error("读取代码文件失败: {}", codeFile, e);
            return false;
        } catch (Exception e) {
            logger.error("索引代码文件失败: {}", codeFile, e);
            return false;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot == -1 ? "" : fileName.substring(lastDot + 1).toLowerCase();
    }
    
    /**
     * 生成文档URL
     */
    private String generateDocumentUrl(Catalogue catalogue) {
        if (StringUtils.hasText(catalogue.getName())) {
            return "/docs/" + catalogue.getName().replaceAll("[^a-zA-Z0-9\\-_]", "_") + ".md";
        }
        return "/docs/untitled.md";
    }
    
    /**
     * 创建Catalogue的元数据
     */
    private Map<String, Object> createCatalogueMetadata(Catalogue catalogue, String taskId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskId", taskId);
        metadata.put("catalogueName", catalogue.getName());
        metadata.put("contentType", "document");
        metadata.put("source", "wiki-generator");
        metadata.put("timestamp", System.currentTimeMillis());
        
        // 添加可用的Catalogue字段
        if (StringUtils.hasText(catalogue.getTitle())) {
            metadata.put("title", catalogue.getTitle());
        }
        if (StringUtils.hasText(catalogue.getPrompt())) {
            metadata.put("prompt", catalogue.getPrompt());
        }
        
        return metadata;
    }
} 