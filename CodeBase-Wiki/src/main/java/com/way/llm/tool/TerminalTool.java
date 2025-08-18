package com.way.llm.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author way
 * @description: 命令行Tool
 * @date 2025/7/17 23:32
 */
@Service
public class TerminalTool {

    @Tool(description = "Windows terminal operation tool")
    public String executeCommand(@ToolParam(description = "The command to be executed in the Windows terminal") String command) {
        StringBuffer outPut = new StringBuffer();
        try {
            // 构建终端命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            Process process = processBuilder.start();
            //同时读取标准输出和错误输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
                 BufferedReader errorReader = new BufferedReader(
                         new InputStreamReader(process.getErrorStream(), "GBK"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    outPut.append(line).append("\n");
                }
                while ((line = errorReader.readLine()) != null) {
                    outPut.append(line).append("\n");
                }
            }
            int exitCode=process.waitFor();
            if(exitCode!=0){
                outPut.append("命令执行失败，并显示退出代码：").append(exitCode);
            }

        } catch (Exception e) {
            return "执行命令时出错: " + e.getMessage();
        }
        return outPut.toString();
    }
}
