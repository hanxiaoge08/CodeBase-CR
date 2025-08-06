package com.hxg.llm.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author hxg
 * @description: 文件系统Tool
 * @date 2025/7/17 17:31
 */
@Service
@Slf4j
public class FileSystemTool {
    
    // 从环境变量或线程变量中获取项目根路径
    private static final ThreadLocal<String> PROJECT_ROOT = new ThreadLocal<>();
    
    // 缓存最近读取的文件路径，避免重复搜索
    private static final ThreadLocal<java.util.Set<String>> READ_FILE_CACHE = ThreadLocal.withInitial(java.util.HashSet::new);
    
    /**
     * 设置当前项目根路径
     */
    public static void setProjectRoot(String projectRoot) {
        if (projectRoot != null && !projectRoot.trim().isEmpty()) {
            PROJECT_ROOT.set(projectRoot.trim());
            // 清空文件读取缓存
            READ_FILE_CACHE.get().clear();
            log.debug("设置项目根路径: {}", projectRoot);
        } else {
            log.warn("尝试设置空的项目根路径，已忽略");
        }
    }
    
    /**
     * 清除当前项目根路径
     */
    public static void clearProjectRoot() {
        PROJECT_ROOT.remove();
        READ_FILE_CACHE.remove();
        log.debug("清除项目根路径ThreadLocal");
    }
    
    /**
     * 获取ThreadLocal状态信息（用于调试）
     */
    public static String getThreadLocalStatus() {
        String projectRoot = PROJECT_ROOT.get();
        java.util.Set<String> cache = READ_FILE_CACHE.get();
        return String.format("ThreadLocal状态: projectRoot=%s, cacheSize=%d", 
                projectRoot != null ? projectRoot : "null", cache.size());
    }
    
    /**
     * 强制重置ThreadLocal（用于异常恢复）
     */
    public static void forceResetThreadLocal() {
        try {
            PROJECT_ROOT.remove();
            READ_FILE_CACHE.remove();
            log.info("强制重置FileSystemTool ThreadLocal状态");
        } catch (Exception e) {
            log.warn("强制重置ThreadLocal时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 获取当前项目根路径
     */
    private String getProjectRoot() {
        String root = PROJECT_ROOT.get();
        if (root == null || root.trim().isEmpty()) {
            log.warn("项目根路径未设置或为空，使用当前工作目录: {}", System.getProperty("user.dir"));
            return System.getProperty("user.dir");
        }
        log.debug("获取项目根路径: {}", root);
        return root;
    }
    
    /**
     * 解析文件路径，支持相对路径和绝对路径
     */
    private File resolveFile(String filePath) {
        Path path = Paths.get(filePath);
        
        if (path.isAbsolute()) {
            // 绝对路径直接使用
            return path.toFile();
        } else {
            // 相对路径需要拼接项目根路径
            String projectRoot = getProjectRoot();
            Path resolvedPath = Paths.get(projectRoot, filePath);
            log.debug("相对路径 {} 解析为绝对路径: {}", filePath, resolvedPath.toString());
            return resolvedPath.toFile();
        }
    }
    
    /**
     * 查找文件，支持模糊匹配
     */
    private File findFile(String filePath) {
        File file = resolveFile(filePath);
        
        // 如果文件存在，直接返回
        if (file.exists()) {
            return file;
        }
        
        // 如果文件不存在，尝试在项目根目录及其子目录中查找
        String projectRoot = getProjectRoot();
        File rootDir = new File(projectRoot);
        
        log.debug("文件 {} 不存在，尝试在项目根目录中查找...", file.getAbsolutePath());
        
        // 递归查找文件
        File foundFile = searchFile(rootDir, new File(filePath).getName());
        if (foundFile != null) {
            log.info("找到匹配文件: {} -> {}", filePath, foundFile.getAbsolutePath());
            return foundFile;
        }
        
        // 如果还是找不到，返回原始文件对象
        return file;
    }
    
    /**
     * 递归搜索文件
     */
    private File searchFile(File directory, String fileName) {
        if (!directory.isDirectory()) {
            return null;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        
        // 首先在当前目录查找
        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return file;
            }
        }
        
        // 递归搜索子目录（限制深度避免过深搜索）
        for (File file : files) {
            if (file.isDirectory() && !file.getName().startsWith(".")) {
                File found = searchFile(file, fileName);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    /**
     * 读取指定文件路径的全部内容
     *
     * @param filePath 文件路径
     * @return 文件内容字符串
     */
    @Tool(name = "readFile", description = "Read the content of the specified file")
    public String readFile(@ToolParam(description = "file path") String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("❌ 文件路径为空");
            return "错误：文件路径不能为空";
        }
        
        String normalizedPath = filePath.trim();
        
        // 检查是否最近已经读取过相同文件，避免重复处理
        java.util.Set<String> cache = READ_FILE_CACHE.get();
        String cacheKey = normalizedPath + "@" + getProjectRoot();
        
        if (cache.contains(cacheKey)) {
            log.debug("⚠️ 检测到重复读取文件: {} (项目根: {})", normalizedPath, getProjectRoot());
        } else {
            cache.add(cacheKey);
        }
        
        StringBuilder content = new StringBuilder();
        
        // 使用改进的文件查找逻辑
        File file = findFile(normalizedPath);
        
        // 记录读取尝试
        log.info("🔍 尝试读取文件: {} (项目根: {})", normalizedPath, getProjectRoot());
        log.debug("📁 文件绝对路径: {}", file.getAbsolutePath());
        log.debug("✅ 文件是否存在: {}", file.exists());
        
        if (!file.exists()) {
            String errorMsg = String.format("文件不存在: %s (绝对路径: %s)", normalizedPath, file.getAbsolutePath());
            log.warn("❌ {}", errorMsg);
            // 对于不存在的文件，返回错误信息而不是抛出异常，让LLM知道文件不存在
            return String.format("错误：文件不存在 - %s\n建议：请检查文件路径是否正确，或者该文件可能不在当前项目中。", normalizedPath);
        }
        
        if (!file.canRead()) {
            String errorMsg = String.format("文件无读取权限: %s", normalizedPath);
            log.error("❌ {}", errorMsg);
            return String.format("错误：文件无读取权限 - %s", normalizedPath);
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            log.info("✅ 文件读取成功: {} (长度: {} 字符)", normalizedPath, content.length());
        } catch (IOException e) {
            String errorMsg = "读取文件失败: " + normalizedPath + ", 错误: " + e.getMessage();
            log.error("❌ {}", errorMsg, e);
            return String.format("错误：读取文件失败 - %s, 原因: %s", normalizedPath, e.getMessage());
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
