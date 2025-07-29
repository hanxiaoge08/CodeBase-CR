package com.hxg.memory.controller;

import com.hxg.memory.dto.BatchDocumentRequest;
import com.hxg.memory.dto.CodeFileRequest;
import com.hxg.memory.dto.DocumentRequest;
import com.hxg.memory.dto.CodeReviewContextRequest;
import com.hxg.memory.dto.ContentTypeSearchRequest;
import com.hxg.memory.dto.MemorySearchResultDto;
import com.hxg.memory.mem0.MemZeroServerRequest;
import com.hxg.memory.mem0.MemZeroServerResp;
import com.hxg.memory.mem0.MemZeroServiceClient;
import com.hxg.memory.service.DocumentMemoryService;
import com.hxg.memory.service.CodeReviewMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆管理API控制器
 * 提供Mem0记忆的CRUD操作接口
 * 
 * @author AI Assistant
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryController.class);
    
    @Autowired
    private MemZeroServiceClient memZeroServiceClient;
    
    @Autowired
    private DocumentMemoryService documentMemoryService;
    
    @Autowired
    private CodeReviewMemoryService codeReviewMemoryService;
    
    /**
     * 添加记忆
     */
    @PostMapping
    public ResponseEntity<String> addMemory(@RequestBody AddMemoryRequest request) {
        
        try {
            logger.info("添加记忆: memoryId={}, messageCount={}", 
                request.getUserId(), request.getMessages().size());
            
            // 构建记忆请求
            MemZeroServerRequest.MemoryCreate memoryCreate = MemZeroServerRequest.MemoryCreate.builder()
                .messages(request.getMessages())
                .userId(request.getUserId())
                .metadata(request.getMetadata())
                .build();
            
            // 添加到Mem0
            memZeroServiceClient.addMemory(memoryCreate);
            
            logger.info("记忆添加成功: memoryId={}", request.getUserId());
            
            return ResponseEntity.ok("记忆添加成功");
            
        } catch (Exception e) {
            logger.error("添加记忆失败: memoryId={}", request.getUserId(), e);
            return ResponseEntity.internalServerError().body("添加记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索记忆
     */
    @PostMapping("/search")
    public ResponseEntity<MemZeroServerResp> searchMemories(@RequestBody SearchMemoryRequest request) {
        
        try {
            logger.info("搜索记忆: memoryId={}, query={}", request.getUserId(), request.getQuery());
            
            // 构建搜索请求
            MemZeroServerRequest.SearchRequest searchRequest = MemZeroServerRequest.SearchRequest.builder()
                .query(request.getQuery())
                .userId(request.getUserId())
                .filters(request.getFilters())
                .build();
            
            // 执行搜索
            MemZeroServerResp result = memZeroServiceClient.searchMemories(searchRequest);
            
            logger.info("记忆搜索完成: memoryId={}, resultCount={}", 
                request.getUserId(), result.getResults() != null ? result.getResults().size() : 0);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("搜索记忆失败: memoryId={}", request.getUserId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有记忆
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<MemZeroServerResp> getAllMemories(@PathVariable("userId") String userId,
                                                          @RequestParam(value = "runId", required = false) String runId,
                                                          @RequestParam(value = "agentId", required = false) String agentId) {
        
        try {
            logger.info("获取用户所有记忆: memoryId={}, runId={}, agentId={}", userId, runId, agentId);
            
            MemZeroServerResp result = memZeroServiceClient.getAllMemories(userId, runId, agentId);
            
            logger.info("获取记忆完成: memoryId={}, memoryCount={}", 
                userId, result.getResults() != null ? result.getResults().size() : 0);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取用户记忆失败: memoryId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取特定记忆
     */
    @GetMapping("/{memoryId}")
    public ResponseEntity<MemZeroServerResp> getMemory(@PathVariable("memoryId") String memoryId) {
        
        try {
            logger.info("获取特定记忆: memoryId={}", memoryId);
            
            MemZeroServerResp result = memZeroServiceClient.getMemory(memoryId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取记忆失败: memoryId={}", memoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 更新记忆
     */
    @PutMapping("/{memoryId}")
    public ResponseEntity<String> updateMemory(@PathVariable("memoryId") String memoryId, 
                                              @RequestBody UpdateMemoryRequest request) {
        
        try {
            logger.info("更新记忆: memoryId={}, memoryId={}", memoryId, request.getMemoryId());
            
            Map<String, Object> updateData = Map.of("memory", request.getMemory());
            Map<String, Object> result = memZeroServiceClient.updateMemory(memoryId, updateData);
            
            logger.info("记忆更新成功: memoryId={}", memoryId);
            
            return ResponseEntity.ok("记忆更新成功");
            
        } catch (Exception e) {
            logger.error("更新记忆失败: memoryId={}, memoryId={}", memoryId, request.getMemoryId(), e);
            return ResponseEntity.internalServerError().body("更新记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 配置Mem0 - 不提供API接口，配置由Spring Boot管理
     */
    /*
    @PostMapping("/configure")
    public ResponseEntity<String> configureMemory(@RequestBody Map<String, Object> config) {
        // 配置由Spring Boot自动管理，不提供API接口
        return ResponseEntity.badRequest().body("配置由系统管理，不支持动态配置");
    }
    */
    
    /**
     * 添加记忆请求
     */
    public static class AddMemoryRequest {
        private List<MemZeroServerRequest.Message> messages;
        private String userId;
        private Map<String, Object> metadata;
        
        // Getters and Setters
        public List<MemZeroServerRequest.Message> getMessages() { return messages; }
        public void setMessages(List<MemZeroServerRequest.Message> messages) { this.messages = messages; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 搜索记忆请求
     */
    public static class SearchMemoryRequest {
        private String query;
        private String userId;
        private Map<String, Object> filters;
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    }
    
    /**
     * 更新记忆请求
     */
    public static class UpdateMemoryRequest {
        private String memory;
        private String memoryId;
        
        // Getters and Setters
        public String getMemory() { return memory; }
        public void setMemory(String memory) { this.memory = memory; }
        
        public String getMemoryId() { return memoryId; }
        public void setMemoryId(String memoryId) { this.memoryId = memoryId; }
    }
    
    // ===============================
    // Wiki集成专用的文档记忆API
    // ===============================
    
    /**
     * 添加单个文档到记忆 (Wiki模块专用)
     */
    @PostMapping("/documents")
    public ResponseEntity<Void> addDocument(@RequestBody DocumentRequest request) {
        try {
            logger.info("接收到添加文档记忆请求: repositoryId={}, documentName={}", 
                request.getRepositoryId(), request.getDocumentName());
            
            documentMemoryService.addDocumentMemoryAsync(
                request.getRepositoryId(),
                request.getDocumentName(),
                request.getDocumentContent(),
                request.getDocumentUrl(),
                request.getMetadata()
            );
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("添加文档记忆失败: repositoryId={}, documentName={}", 
                request.getRepositoryId(), request.getDocumentName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量添加文档到记忆 (Wiki模块专用)
     */
    @PostMapping("/documents/batch")
    public ResponseEntity<Void> batchAddDocuments(@RequestBody BatchDocumentRequest request) {
        try {
            logger.info("接收到批量添加文档记忆请求: repositoryId={}, documentCount={}", 
                request.getRepositoryId(), request.getDocuments().size());
            
            // 转换DTO到服务层对象
            List<DocumentMemoryService.DocumentInfo> documents = request.getDocuments().stream()
                .map(dto -> new DocumentMemoryService.DocumentInfo(
                    dto.getName(), 
                    dto.getContent(), 
                    dto.getUrl(), 
                    dto.getMetadata()))
                .collect(Collectors.toList());
            
            documentMemoryService.batchAddDocumentMemoriesAsync(request.getRepositoryId(), documents);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("批量添加文档记忆失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 添加代码文件到记忆 (Wiki模块专用)
     */
    @PostMapping("/code-files")
    public ResponseEntity<Void> addCodeFile(@RequestBody CodeFileRequest request) {
        try {
            logger.info("接收到添加代码文件记忆请求: repositoryId={}, fileName={}", 
                request.getRepositoryId(), request.getFileName());
            
            documentMemoryService.addCodeFileMemoryAsync(
                request.getRepositoryId(),
                request.getFileName(),
                request.getFilePath(),
                request.getFileContent(),
                request.getFileType()
            );
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("添加代码文件记忆失败: repositoryId={}, fileName={}", 
                request.getRepositoryId(), request.getFileName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ===============================
    // Review集成专用的代码评审记忆API
    // ===============================
    
    /**
     * 为代码评审搜索相关上下文 (Review模块专用)
     */
    @PostMapping("/code-review/context")
    public ResponseEntity<String> searchContextForCodeReview(@RequestBody CodeReviewContextRequest request) {
        try {
            logger.info("接收到代码评审上下文搜索请求: repositoryId={}", request.getRepositoryId());
            
            String context = codeReviewMemoryService.searchContextForCodeReview(
                request.getRepositoryId(),
                request.getDiffContent(),
                request.getPrTitle(),
                request.getPrDescription(),
                request.getChangedFiles()
            );
            
            return ResponseEntity.ok(context);
            
        } catch (Exception e) {
            logger.error("代码评审上下文搜索失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().body("搜索上下文失败: " + e.getMessage());
        }
    }
    
    /**
     * 按内容类型搜索 (Review模块专用)
     */
    @PostMapping("/code-review/search")
    public ResponseEntity<MemorySearchResultDto.SearchResponse> searchByContentType(@RequestBody ContentTypeSearchRequest request) {
        try {
            logger.info("接收到按内容类型搜索请求: repositoryId={}, contentType={}", 
                request.getRepositoryId(), request.getContentType());
            
            List<CodeReviewMemoryService.MemorySearchResult> results = codeReviewMemoryService.searchByContentType(
                request.getRepositoryId(),
                request.getQuery(),
                request.getContentType(),
                request.getLimit()
            );
            
            // 转换为DTO
            List<MemorySearchResultDto> dtoResults = results.stream()
                .map(result -> new MemorySearchResultDto(
                    result.getId(),
                    result.getContent(),
                    result.getScore(),
                    result.getType(),
                    result.getName(),
                    result.getMetadata()
                ))
                .collect(Collectors.toList());
            
            MemorySearchResultDto.SearchResponse response = new MemorySearchResultDto.SearchResponse(dtoResults, dtoResults.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("按内容类型搜索失败: repositoryId={}, contentType={}", 
                request.getRepositoryId(), request.getContentType(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 搜索相关文档 (Review模块专用)
     */
    @PostMapping("/code-review/search/documents")
    public ResponseEntity<MemorySearchResultDto.SearchResponse> searchRelatedDocuments(@RequestBody ContentTypeSearchRequest request) {
        try {
            logger.info("接收到搜索相关文档请求: repositoryId={}", request.getRepositoryId());
            
            List<CodeReviewMemoryService.MemorySearchResult> results = codeReviewMemoryService.searchRelatedDocuments(
                request.getRepositoryId(),
                request.getQuery(),
                request.getLimit()
            );
            
            List<MemorySearchResultDto> dtoResults = results.stream()
                .map(result -> new MemorySearchResultDto(
                    result.getId(),
                    result.getContent(),
                    result.getScore(),
                    result.getType(),
                    result.getName(),
                    result.getMetadata()
                ))
                .collect(Collectors.toList());
            
            MemorySearchResultDto.SearchResponse response = new MemorySearchResultDto.SearchResponse(dtoResults, dtoResults.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("搜索相关文档失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 搜索相关代码文件 (Review模块专用)
     */
    @PostMapping("/code-review/search/code-files")
    public ResponseEntity<MemorySearchResultDto.SearchResponse> searchRelatedCodeFiles(@RequestBody ContentTypeSearchRequest request) {
        try {
            logger.info("接收到搜索相关代码文件请求: repositoryId={}", request.getRepositoryId());
            
            List<CodeReviewMemoryService.MemorySearchResult> results = codeReviewMemoryService.searchRelatedCodeFiles(
                request.getRepositoryId(),
                request.getQuery(),
                request.getLimit()
            );
            
            List<MemorySearchResultDto> dtoResults = results.stream()
                .map(result -> new MemorySearchResultDto(
                    result.getId(),
                    result.getContent(),
                    result.getScore(),
                    result.getType(),
                    result.getName(),
                    result.getMetadata()
                ))
                .collect(Collectors.toList());
            
            MemorySearchResultDto.SearchResponse response = new MemorySearchResultDto.SearchResponse(dtoResults, dtoResults.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("搜索相关代码文件失败: repositoryId={}", request.getRepositoryId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 