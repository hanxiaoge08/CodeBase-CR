package com.hxg.service.impl;

import com.alibaba.example.chatmemory.service.DocumentMemoryService;
import com.hxg.model.entity.Catalogue;
import com.hxg.service.IMemoryIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
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
 * 
 * @author AI Assistant
 */
@Service
@ConditionalOnClass(DocumentMemoryService.class)
public class MemoryIntegrationServiceImpl implements IMemoryIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryIntegrationServiceImpl.class);
    
    @Autowired(required = false)
    private DocumentMemoryService documentMemoryService;
    
    // 支持的代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = Set.of(
        "java", "js", "ts", "py", "go", "cpp", "c", "h", "cs", "php", "rb", "kt", "swift"
    );
    
    // 支持的文档文件扩展名
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
        "md", "txt", "rst", "adoc", "org"
    );
    
    @Override
    public CompletableFuture<Void> indexProjectToMemoryAsync(String taskId, 
                                                            List<Catalogue> catalogues, 
                                                            String projectPath) {
        
        if (documentMemoryService == null) {
            logger.warn("DocumentMemoryService不可用，跳过记忆索引: taskId={}", taskId);
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始项目记忆索引: taskId={}, catalogueCount={}, projectPath={}", 
                    taskId, catalogues.size(), projectPath);
                
                // 索引生成的文档内容
                List<DocumentMemoryService.DocumentInfo> documents = new ArrayList<>();
                
                for (Catalogue catalogue : catalogues) {
                    if (StringUtils.hasText(catalogue.getContent())) {
                        String documentUrl = generateDocumentUrl(catalogue);
                        Map<String, Object> metadata = createCatalogueMetadata(catalogue, taskId);
                        
                        DocumentMemoryService.DocumentInfo docInfo = new DocumentMemoryService.DocumentInfo(
                            catalogue.getName(),
                            catalogue.getContent(),
                            documentUrl,
                            metadata
                        );
                        documents.add(docInfo);
                    }
                }
                
                // 批量索引文档
                if (!documents.isEmpty()) {
                    documentMemoryService.batchAddDocumentMemoriesAsync(taskId, documents)
                        .exceptionally(ex -> {
                            logger.error("批量索引文档失败: taskId={}", taskId, ex);
                            return null;
                        });
                }
                
                // 并行索引代码文件
                CompletableFuture<Void> codeIndexFuture = indexCodeFilesToMemoryAsync(taskId, projectPath);
                
                // 等待代码文件索引完成
                codeIndexFuture.join();
                
                logger.info("项目记忆索引完成: taskId={}, 文档数量={}", taskId, documents.size());
                
            } catch (Exception e) {
                logger.error("项目记忆索引失败: taskId={}", taskId, e);
                throw new RuntimeException("项目记忆索引失败", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> indexDocumentToMemoryAsync(String taskId, Catalogue catalogue) {
        
        if (documentMemoryService == null) {
            logger.debug("DocumentMemoryService不可用，跳过文档记忆索引: taskId={}", taskId);
            return CompletableFuture.completedFuture(null);
        }
        
        if (!StringUtils.hasText(catalogue.getContent())) {
            logger.debug("文档内容为空，跳过索引: taskId={}, catalogueName={}", taskId, catalogue.getName());
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("开始单个文档记忆索引: taskId={}, catalogueName={}", taskId, catalogue.getName());
                
                String documentUrl = generateDocumentUrl(catalogue);
                Map<String, Object> metadata = createCatalogueMetadata(catalogue, taskId);
                
                documentMemoryService.addDocumentMemoryAsync(
                    taskId,
                    catalogue.getName(),
                    catalogue.getContent(),
                    documentUrl,
                    metadata
                ).exceptionally(ex -> {
                    logger.error("单个文档索引失败: taskId={}, catalogueName={}", taskId, catalogue.getName(), ex);
                    return null;
                });
                
                logger.debug("单个文档记忆索引完成: taskId={}, catalogueName={}", taskId, catalogue.getName());
                
            } catch (Exception e) {
                logger.error("单个文档记忆索引异常: taskId={}, catalogueName={}", taskId, catalogue.getName(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> indexCodeFilesToMemoryAsync(String taskId, String projectPath) {
        
        if (documentMemoryService == null) {
            logger.info("DocumentMemoryService不可用，跳过代码文件记忆索引: taskId={}", taskId);
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始代码文件记忆索引: taskId={}, projectPath={}", taskId, projectPath);
                
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
                        if (indexSingleCodeFile(taskId, projectDir, codeFile)) {
                            indexedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("索引代码文件失败: {}", codeFile, e);
                    }
                }
                
                logger.info("代码文件记忆索引完成: taskId={}, 总文件数={}, 索引数={}", 
                    taskId, codeFiles.size(), indexedCount);
                
            } catch (Exception e) {
                logger.error("代码文件记忆索引异常: taskId={}", taskId, e);
            }
        });
    }
    
    @Override
    public boolean isMemoryServiceAvailable() {
        return documentMemoryService != null;
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
    private boolean indexSingleCodeFile(String taskId, Path projectDir, Path codeFile) {
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
            
            // 异步索引
            documentMemoryService.addCodeFileMemoryAsync(
                taskId,
                fileName,
                relativePath,
                content,
                fileExtension
            ).exceptionally(ex -> {
                logger.error("代码文件索引失败: {}", relativePath, ex);
                return null;
            });
            
            return true;
            
        } catch (IOException e) {
            logger.error("读取代码文件失败: {}", codeFile, e);
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