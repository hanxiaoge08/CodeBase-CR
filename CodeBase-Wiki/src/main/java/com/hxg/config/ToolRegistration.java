package com.hxg.config;

import com.hxg.llm.tool.FileSystemTool;
import com.hxg.llm.tool.TerminalTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hxg
 * @description: Too注册配置
 * @date 2025/7/18 00:26
 */
@Configuration
public class ToolRegistration {
    @Bean
    public ToolCallback[] allTools() {
        FileSystemTool fileSystemTool = new FileSystemTool();
        TerminalTool terminalTool = new TerminalTool();
        return ToolCallbacks.from(
                fileSystemTool,
                terminalTool
        );
    }
}
