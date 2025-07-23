package com.hxg.controller;

import com.hxg.llm.service.LlmService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
class ChatController {
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private ToolCallback[] allTools;
    
    @Autowired
    private LlmService llmService;

    @GetMapping(value="/call", produces = "application/json;charset=UTF-8")
    public String callChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        return chatClientBuilder
                .defaultOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(allTools)
                        .build()
                )
                .build()
                .prompt(query)
                .call()
                .content();
    }


    @GetMapping(value="/callTools", produces = "application/json;charset=UTF-8")
    public String callChatWithTools(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        String result1= llmService.callWithoutTools(query);
        String result2= llmService.callWithTools(query);
        return result1 + "\n" + result2;
    }

}
