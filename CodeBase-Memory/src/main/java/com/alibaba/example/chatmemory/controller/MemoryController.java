package com.alibaba.example.chatmemory.controller;

import com.alibaba.example.chatmemory.mem0.MemZeroServerRequest;
import com.alibaba.example.chatmemory.mem0.MemZeroServerResp;
import com.alibaba.example.chatmemory.mem0.MemZeroServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    
    /**
     * 添加记忆
     */
    @PostMapping
    public ResponseEntity<String> addMemory(@RequestBody AddMemoryRequest request) {
        
        try {
            logger.info("添加记忆: userId={}, messageCount={}", 
                request.getUserId(), request.getMessages().size());
            
            // 构建记忆请求
            MemZeroServerRequest.MemoryCreate memoryCreate = MemZeroServerRequest.MemoryCreate.builder()
                .messages(request.getMessages())
                .userId(request.getUserId())
                .metadata(request.getMetadata())
                .build();
            
            // 添加到Mem0
            memZeroServiceClient.addMemory(memoryCreate);
            
            logger.info("记忆添加成功: userId={}", request.getUserId());
            
            return ResponseEntity.ok("记忆添加成功");
            
        } catch (Exception e) {
            logger.error("添加记忆失败: userId={}", request.getUserId(), e);
            return ResponseEntity.internalServerError().body("添加记忆失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索记忆
     */
    @PostMapping("/search")
    public ResponseEntity<MemZeroServerResp> searchMemories(@RequestBody SearchMemoryRequest request) {
        
        try {
            logger.info("搜索记忆: userId={}, query={}", request.getUserId(), request.getQuery());
            
            // 构建搜索请求
            MemZeroServerRequest.SearchRequest searchRequest = MemZeroServerRequest.SearchRequest.builder()
                .query(request.getQuery())
                .userId(request.getUserId())
                .filters(request.getFilters())
                .build();
            
            // 执行搜索
            MemZeroServerResp result = memZeroServiceClient.searchMemories(searchRequest);
            
            logger.info("记忆搜索完成: userId={}, resultCount={}", 
                request.getUserId(), result.getResults() != null ? result.getResults().size() : 0);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("搜索记忆失败: userId={}", request.getUserId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有记忆
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<MemZeroServerResp> getAllMemories(@PathVariable String userId,
                                                          @RequestParam(required = false) String runId,
                                                          @RequestParam(required = false) String agentId) {
        
        try {
            logger.info("获取用户所有记忆: userId={}, runId={}, agentId={}", userId, runId, agentId);
            
            MemZeroServerResp result = memZeroServiceClient.getAllMemories(userId, runId, agentId);
            
            logger.info("获取记忆完成: userId={}, memoryCount={}", 
                userId, result.getResults() != null ? result.getResults().size() : 0);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取用户记忆失败: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取特定记忆
     */
    @GetMapping("/{memoryId}")
    public ResponseEntity<MemZeroServerResp> getMemory(@PathVariable String memoryId) {
        
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
    public ResponseEntity<String> updateMemory(@PathVariable String memoryId, 
                                              @RequestBody UpdateMemoryRequest request) {
        
        try {
            logger.info("更新记忆: memoryId={}, userId={}", memoryId, request.getUserId());
            
            Map<String, Object> updateData = Map.of("text", request.getData());
            Map<String, Object> result = memZeroServiceClient.updateMemory(memoryId, updateData);
            
            logger.info("记忆更新成功: memoryId={}", memoryId);
            
            return ResponseEntity.ok("记忆更新成功");
            
        } catch (Exception e) {
            logger.error("更新记忆失败: memoryId={}, userId={}", memoryId, request.getUserId(), e);
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
        private String data;
        private String userId;
        
        // Getters and Setters
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
} 