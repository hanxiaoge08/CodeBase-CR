package com.way.service.impl;

import com.way.model.dto.CodeParseResponse;
import com.way.model.entity.Catalogue;
import com.way.model.es.CodeChunk;
import com.way.model.es.DocumentIndex;
import com.way.service.CodeParseService;
import com.way.service.ElasticsearchIndexService;
import com.way.service.IESIntegrationService;
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
public class ESIntegrationServiceImpl implements IESIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ESIntegrationServiceImpl.class);
    
    @Autowired
    private CodeParseService codeParseService;
    
    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;
    
    // 支持的代码文件扩展名
    private static final Set<String> CODE_EXTENSIONS = Set.of(
        "java", "js", "ts", "py", "go", "cpp", "c", "h", "cs", "php", "rb", "kt", "swift"
    );

    @Override
    public CompletableFuture<Void> indexCodeFilesToESAsync(String repositoryId, String taskId, String projectPath) {

        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始代码文件ES索引: repositoryId={}, taskId={}, projectPath={}", repositoryId, taskId, projectPath);

                Path projectDir = Paths.get(projectPath);
                if (!Files.exists(projectDir) || !Files.isDirectory(projectDir)) {
                    logger.warn("项目路径不存在或不是目录: {}", projectPath);
                    return;
                }

                // 扫描代码文件
                List<Path> codeFiles = scanCodeFiles(projectDir);
                logger.info("扫描到代码文件数量: {}", codeFiles.size());

                int indexedCount = 0;
                int failedCount = 0;
                
                for (Path codeFile : codeFiles) {
                    try {
                        if (indexSingleCodeFileToES(repositoryId, taskId, projectDir, codeFile)) {
                            indexedCount++;
                        } else {
                            failedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("索引代码文件失败: {}", codeFile, e);
                        failedCount++;
                    }
                }

                logger.info("代码文件ES索引完成: repositoryId={}, 总文件数={}, 成功索引={}, 失败={}",
                        repositoryId, codeFiles.size(), indexedCount, failedCount);

            } catch (Exception e) {
                logger.error("代码文件ES索引异常: repositoryId={}", repositoryId, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> indexDocumentToESAsync(String repositoryId, Catalogue catalogue) {
        
        if (!StringUtils.hasText(catalogue.getContent())) {
            logger.debug("文档内容为空，跳过索引: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("开始单个文档ES索引: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
                
                // 转换为ES文档索引模型
                DocumentIndex documentIndex = convertToDocumentIndex(repositoryId, catalogue);
                
                // 异步索引到ES
                Boolean indexResult = elasticsearchIndexService.indexDocument(documentIndex).join();
                
                if (indexResult) {
                    logger.debug("单个文档ES索引成功: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
                } else {
                    logger.warn("单个文档ES索引失败: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName());
                }
                
            } catch (Exception e) {
                logger.error("单个文档ES索引异常: repositoryId={}, catalogueName={}", repositoryId, catalogue.getName(), e);
            }
        });
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

    //todo 上传代码文件到es 一次一个文件 访问http://localhost:8566/parse  传递三个参数：  "language": "java",
    //  "code":""  "max_chars": 1000
    //返回格式为{
    //  "chunks": [
    //    {
    //      "id": "chunk:1",
    //      "language": "java",
    //      "subType": "function",
    //      "className": "DemoLargeClass",
    //      "methodName": "DemoLargeClass",
    //      "apiName": "DemoLargeClass#DemoLargeClass",
    //      "docSummary": "import java.io.*;\nimport java.util.*;\nimport java.util.stream.*;\n\npublic class DemoLargeClass {\n\n    /** 构造器 */",
    //      "content": "public DemoLargeClass() {\n        System.out.println(\"DemoLargeClass init\");\n    }"
    //    },
    //    {
    //      "id": "chunk:2",
    //      "language": "java",
    //      "subType": "function",
    //      "className": "DemoLargeClass",
    //      "methodName": "sum",
    //      "apiName": "DemoLargeClass#sum",
    //      "docSummary": "/** 构造器 */\n    public DemoLargeClass() {\n        System.out.println(\"DemoLargeClass init\");\n    }\n\n    /** 简单求和 */",
    //      "content": "public int sum(List<Integer> nums) {\n        int s = 0;\n        for (int n : nums) {\n            s += n;\n        }\n        return s;\n    }"
    //    }]  写入es 支持混合索引


    /**
     * 索引单个代码文件到ES
     */
    private boolean indexSingleCodeFileToES(String repositoryId, String taskId, Path projectDir, Path codeFile) {
        try {
            // 读取文件内容
            String content = Files.readString(codeFile);
            if (content.trim().isEmpty()) {
                logger.debug("文件内容为空，跳过索引: {}", codeFile);
                return false;
            }
            
            // 计算相对路径
            String relativePath = projectDir.relativize(codeFile).toString().replace('\\', '/');
            String fileName = codeFile.getFileName().toString();
            
            // 推断编程语言
            String language = codeParseService.inferLanguageFromFileName(fileName);
            
            logger.debug("开始解析代码文件: file={}, language={}, size={}", 
                    relativePath, language, content.length());
            
            // 调用代码解析服务
            CodeParseResponse parseResponse = codeParseService.parseCode(language, content, 1000);
            
            if (parseResponse.getChunks() == null || parseResponse.getChunks().isEmpty()) {
                logger.debug("代码文件解析结果为空: {}", relativePath);
                return false;
            }
            
            // 索引每个代码块
            int successCount = 0;
                            for (CodeParseResponse.CodeChunkDto chunkDto : parseResponse.getChunks()) {
                try {
                    // 转换为ES模型
                    CodeChunk codeChunk = convertToCodeChunk(repositoryId, taskId, relativePath, chunkDto);
                    
                    // 异步索引到ES
                    Boolean indexResult = elasticsearchIndexService.indexCodeChunk(codeChunk).join();
                    if (indexResult) {
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    logger.error("索引代码块失败: file={}, chunkId={}, error={}", 
                            relativePath, chunkDto.getId(), e.getMessage());
                }
            }
            
            logger.debug("代码文件索引完成: file={}, totalChunks={}, successCount={}", 
                    relativePath, parseResponse.getChunks().size(), successCount);
            
            return successCount > 0;
            
        } catch (IOException e) {
            logger.error("读取代码文件失败: {}", codeFile, e);
            return false;
        } catch (Exception e) {
            logger.error("索引代码文件到ES失败: {}", codeFile, e);
            return false;
        }
    }
    
    /**
     * 转换为ES代码块模型
     */
    private CodeChunk convertToCodeChunk(String repositoryId, String taskId, String filePath, CodeParseResponse.CodeChunkDto chunkDto) {
        CodeChunk codeChunk = new CodeChunk();
        codeChunk.setRepoId(repositoryId != null ? repositoryId : "unknown");
        codeChunk.setTaskId(taskId != null ? taskId : "unknown"); // 正确设置taskId
        codeChunk.setLanguage(chunkDto.getLanguage() != null ? chunkDto.getLanguage() : "unknown");
        codeChunk.setClassName(chunkDto.getClassName());
        codeChunk.setMethodName(chunkDto.getMethodName());
        // 处理apiName，确保不为null
        if (chunkDto.getApiName() != null) {
            codeChunk.setApiName(chunkDto.getApiName());
        } else {
            String className = chunkDto.getClassName() != null ? chunkDto.getClassName() : "Unknown";
            String methodName = chunkDto.getMethodName() != null ? chunkDto.getMethodName() : "unknown";
            codeChunk.setApiName(className + "#" + methodName);
        }
        codeChunk.setDocSummary(chunkDto.getDocSummary());
        codeChunk.setContent(chunkDto.getContent() != null ? chunkDto.getContent() : "");
        
        // 生成文档SHA256 (基于内容和路径)
        String content = chunkDto.getContent() != null ? chunkDto.getContent() : "";
        String combinedContent = filePath + ":" + content;
        codeChunk.setSha256(org.apache.commons.codec.digest.DigestUtils.sha256Hex(combinedContent));
        
        return codeChunk;
    }

    /**
     * 转换为ES文档索引模型
     */
    private DocumentIndex convertToDocumentIndex(String repositoryId, Catalogue catalogue) {
        DocumentIndex documentIndex = new DocumentIndex();
        // 正确设置repoId: 如果catalogue有repoId则使用，否则使用repositoryId
        String repoId = catalogue.getRepoId() != null ? catalogue.getRepoId() : repositoryId;
        documentIndex.setRepoId(repoId != null ? repoId : "unknown");
        documentIndex.setTaskId(catalogue.getTaskId() != null ? catalogue.getTaskId() : "unknown");
        documentIndex.setCatalogueId(catalogue.getCatalogueId() != null ? catalogue.getCatalogueId() : "unknown");
        documentIndex.setName(catalogue.getName() != null ? catalogue.getName() : "unknown");
        documentIndex.setContent(catalogue.getContent() != null ? catalogue.getContent() : "");
        documentIndex.setStatus(catalogue.getStatus() != null ? catalogue.getStatus() : 0);
        
        // 生成文档SHA256
        String content = catalogue.getContent() != null ? catalogue.getContent() : "";
        documentIndex.setSha256(org.apache.commons.codec.digest.DigestUtils.sha256Hex(content));
        
        return documentIndex;
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