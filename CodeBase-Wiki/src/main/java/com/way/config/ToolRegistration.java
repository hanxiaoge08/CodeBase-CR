package com.way.config;

import com.way.llm.tool.FileSystemTool;
// import com.way.llm.tool.TerminalTool;  // 暂时禁用，Wiki文档生成不需要命令行工具
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author way
 * @description: Tool注册配置
 * @date 2025/7/18 00:26
 */
@Configuration
public class ToolRegistration {
    @Bean
    public ToolCallback[] allTools() {
        FileSystemTool fileSystemTool = new FileSystemTool();
        // TerminalTool terminalTool = new TerminalTool();  // 暂时禁用
        return ToolCallbacks.from(
                fileSystemTool
                // terminalTool  // 暂时禁用
        );
    }
}
