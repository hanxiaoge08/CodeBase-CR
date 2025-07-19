package com.hxg.llm.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author hxg
 * @description: 文件系统Tool
 * @date 2025/7/17 17:31
 */
@Service
public class FileSystemTool {
    /**
     * 读取指定文件路径的全部内容
     *
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    @Tool(name = "readFile", description = "Read the content of the specified file")
    public String readFile(@ToolParam(description = "file path") String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败" + e.getMessage(), e);
        }
        return content.toString();
    }

    /**
     * 读取指定文件路径的指定行区间内容（包含startLine和endLine，行号从1开始）
     * @param filePath 文件路径
     * @param startLine 起始行号（从1开始）
     * @param endLine 结束行号（从1开始）
     * @return 指定区间的内容字符串
     */
    @Tool(name = "readFileLines", description = "Read the content of the specified file within a specified line range")
    public String readFileLines(@ToolParam(description = "file path") String filePath, @ToolParam(description = "start line") int startLine, @ToolParam(description = "end line") int endLine) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            int currentLine = 1;
            while ((line = reader.readLine()) != null) {
                if (currentLine >= startLine && currentLine <= endLine) {
                    content.append(line).append("\n");
                }
                if (currentLine > endLine) {
                    break;
                }
                currentLine++;
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件指定行失败" + e.getMessage(), e);
        }
        return content.toString();
    }

}
