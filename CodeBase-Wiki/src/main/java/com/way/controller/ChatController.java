package com.way.controller;

import com.way.llm.service.LlmService;
import com.way.model.dto.*;
import com.way.model.vo.ResultVo;
import com.way.service.CodeReviewESService;
import com.way.service.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private LlmService llmService;
    
    @Autowired
    private RAGService ragService;

    @Autowired
    CodeReviewESService codeReviewESService;

    @GetMapping(value="/call", produces = "application/json;charset=UTF-8")
    public ResultVo<ChatResponseDTO> callChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query,
                                             @RequestParam(value = "taskId", required = false) String taskId,
                                             @RequestParam(value = "useRAG", defaultValue = "true") boolean useRAG,
                                             @RequestParam(value = "topK", required = false) Integer topK) {
        
        try {
            ChatResponseDTO response = ragService.processChat(query, taskId, useRAG, topK);
            return ResultVo.success(response);
            
        } catch (Exception e) {
            return ResultVo.error("对话处理失败：" + e.getMessage());
        }
    }


    @GetMapping(value="/callTools", produces = "application/json;charset=UTF-8")
    public ResultVo<ToolCallResponseDTO> callChatWithTools(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        try {
            String withoutToolsResult = llmService.callWithoutTools(query);
            String withToolsResult = llmService.callWithTools(query);
            
            ToolCallResponseDTO response = new ToolCallResponseDTO();
            response.setQuery(query);
            response.setWithoutToolsResult(withoutToolsResult);
            response.setWithToolsResult(withToolsResult);
            
            return ResultVo.success(response);
            
        } catch (Exception e) {
            return ResultVo.error("工具调用失败：" + e.getMessage());
        }
    }

    /**
     * RAG检索接口 - 返回检索结果和AI回答
     */
    @GetMapping(value="/search", produces = "application/json;charset=UTF-8")
    public ResultVo<RAGSearchResponseDTO> searchWithRAG(@RequestParam(value = "query") String query,
                                                        @RequestParam(value = "taskId", required = false) String taskId,
                                                        @RequestParam(value = "topK", defaultValue = "10") Integer topK) {
        
        try {
            RAGSearchResponseDTO response = ragService.processSearchWithRAG(query, taskId, topK);
            return ResultVo.success(response);
            
        } catch (Exception e) {
            return ResultVo.error("检索过程中发生错误：" + e.getMessage());
        }
    }


    @GetMapping(value="/searchOnly", produces = "application/json;charset=UTF-8")
    public ResultVo<SearchOnlyResponseDTO> searchOnly(@RequestParam(value = "query") String query,
                                                      @RequestParam(value = "taskId", required = false) String taskId,
                                                      @RequestParam(value = "topK", defaultValue = "10") Integer topK) {
        
        try {
            SearchOnlyResponseDTO response = ragService.processSearchOnly(query, taskId, topK);
            return ResultVo.success(response);
            
        } catch (Exception e) {
            return ResultVo.error("检索过程中发生错误：" + e.getMessage());
        }
    }

    /**
     * 为代码评审搜索相关上下文
     * 供OpenFeign远程调用使用
     */
    @PostMapping("/reviewContext")
    public String searchContextForCodeReview(@RequestBody CodeReviewContextRequest request) {
        try {
            logger.info("接收到代码评审上下文请求: {}", request);
            return codeReviewESService.searchContextForCodeReview(request);
        } catch (Exception e) {
            logger.error("代码评审上下文检索失败: {}", e.getMessage(), e);
            return "";
        }
    }

}
