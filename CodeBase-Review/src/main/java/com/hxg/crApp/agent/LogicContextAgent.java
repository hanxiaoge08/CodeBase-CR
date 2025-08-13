package com.hxg.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.hxg.crApp.dto.review.ReviewCommentDTO;
import com.hxg.crApp.service.ReviewMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 逻辑与上下文审查员Agent - 集成Memory服务检查代码逻辑和设计模式
 * <p>
 * 职责：
 * 1. 通过Memory服务获取项目上下文
 * 2. 检查代码逻辑是否存在明显漏洞（空指针、死循环等）
 * 3. 检查新增代码是否与项目现有设计模式一致
 * 4. 检查是否存在重复造轮子的情况
 * 5. 评估代码变更对现有功能的潜在影响
 * @author hxg
 */
@Component
public class LogicContextAgent implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(LogicContextAgent.class);

    @Autowired
    private ChatClient chatClient;

    @Autowired(required = false)
    private ReviewMemoryService memoryService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("逻辑与上下文审查员Agent开始执行");

        String diffContent = (String) state.value("diff_content").orElse("");
        String repoName = (String) state.value("repo_name").orElse("");
        String prTitle = (String) state.value("pr_title").orElse("");
        String[] changedFiles = (String[]) state.value("changed_files").orElse(new String[0]);

        List<ReviewCommentDTO> logicIssues = new ArrayList<>();

        // 1. 从Memory服务获取项目上下文
        String projectContext = retrieveProjectContext(repoName, diffContent, prTitle, changedFiles);
        if (projectContext != null) {
            logger.info("获取到项目上下文，长度: {}", projectContext.length());
        }

        // 2. 解析diff内容
        Map<String, List<String>> fileChanges = parseDiffByFile(diffContent);

        for (Map.Entry<String, List<String>> entry : fileChanges.entrySet()) {
            String filePath = entry.getKey();
            List<String> changes = entry.getValue();

            // 只检查Java文件
            if (!filePath.endsWith(".java")) {
                continue;
            }

            logger.debug("检查文件逻辑: {}", filePath);

            // 3. 基础逻辑检查
            logicIssues.addAll(checkBasicLogicIssues(filePath, changes));

            // 4. 空指针风险检查
            logicIssues.addAll(checkNullPointerRisks(filePath, changes));

            // 5. 资源泄露检查
            logicIssues.addAll(checkResourceLeaks(filePath, changes));

            // 6. 使用LLM结合项目上下文进行深度分析
            if (!changes.isEmpty() && projectContext != null) {
                logicIssues.addAll(performContextualAnalysis(filePath, changes, projectContext));
            }
        }

        // 7. 检查设计模式一致性
        logicIssues.addAll(checkDesignPatternConsistency(diffContent, projectContext));

        // 8. 检查是否有重复功能
        logicIssues.addAll(checkDuplicateImplementation(diffContent, projectContext));

        Map<String, Object> result = new HashMap<>();
        result.put("logic_issues", logicIssues);
        result.put("logic_issues_count", logicIssues.size());
        result.put("has_project_context", projectContext != null && !projectContext.isEmpty());

        // 按严重程度分类统计
        long errors = logicIssues.stream().filter(i -> "error".equals(i.severity())).count();
        long warnings = logicIssues.stream().filter(i -> "warning".equals(i.severity())).count();
        long infos = logicIssues.stream().filter(i -> "info".equals(i.severity())).count();

        result.put("logic_error_count", errors);
        result.put("logic_warning_count", warnings);
        result.put("logic_info_count", infos);

        logger.info("逻辑审查完成，发现问题: {} (错误:{}, 警告:{}, 信息:{})",
                logicIssues.size(), errors, warnings, infos);

        return result;
    }

    /**
     * 从Memory服务获取项目上下文
     */
    private String retrieveProjectContext(String repoName, String diffContent,
                                          String prTitle, String[] changedFiles) {
        try {
            if (memoryService == null) {
                logger.warn("Memory服务不可用，跳过上下文检索");
                return null;
            }

            // 先检查Memory服务是否可用
            if (!memoryService.isMemoryServiceAvailable()) {
                logger.warn("Memory服务当前不可访问，将使用降级模式");
                return buildFallbackContext(repoName, prTitle, changedFiles);
            }

            logger.info("调用Memory服务获取项目上下文: {}", repoName);

            // 提取变更的关键代码片段
            String codeSnippet = extractKeyCodeSnippets(diffContent);

            // 调用Memory服务 - 添加超时重试机制
            String context = null;
            int retryCount = 0;
            int maxRetries = 2;
            
            while (context == null && retryCount < maxRetries) {
                try {
                    context = memoryService.searchContextForCodeReview(
                            repoName,
                            codeSnippet,
                            prTitle,
                            "检查代码逻辑和设计模式一致性",
                            Arrays.asList(changedFiles)
                    );
                    
                    if (context != null && !context.isEmpty()) {
                        logger.info("成功获取项目上下文");
                        return context;
                    }
                } catch (Exception innerEx) {
                    retryCount++;
                    if (retryCount >= maxRetries) {
                        logger.error("Memory服务调用失败，已重试{}次", retryCount, innerEx);
                        return buildFallbackContext(repoName, prTitle, changedFiles);
                    }
                    logger.warn("Memory服务调用失败，重试 {}/{}", retryCount, maxRetries);
                    Thread.sleep(1000 * retryCount); // 递增等待时间
                }
            }

            return context;

        } catch (Exception e) {
            logger.error("获取项目上下文失败，使用降级模式", e);
            return buildFallbackContext(repoName, prTitle, changedFiles);
        }
    }
    
    /**
     * 构建降级的上下文信息
     */
    private String buildFallbackContext(String repoName, String prTitle, String[] changedFiles) {
        StringBuilder context = new StringBuilder();
        context.append("=== 项目上下文信息（降级模式）===\n");
        context.append("仓库: ").append(repoName).append("\n");
        context.append("PR标题: ").append(prTitle).append("\n");
        context.append("变更文件数: ").append(changedFiles.length).append("\n");
        
        if (changedFiles.length > 0 && changedFiles.length <= 10) {
            context.append("变更文件列表:\n");
            for (String file : changedFiles) {
                context.append("  - ").append(file).append("\n");
            }
        }
        
        context.append("\n注意：Memory服务暂时不可用，基于代码变更内容进行分析。\n");
        context.append("建议：\n");
        context.append("1. 确保Memory服务已启动（端口8080）\n");
        context.append("2. 检查网络连接是否正常\n");
        context.append("3. 验证服务配置是否正确\n");
        
        return context.toString();
    }

    /**
     * 提取关键代码片段
     */
    private String extractKeyCodeSnippets(String diffContent) {
        StringBuilder snippets = new StringBuilder();
        String[] lines = diffContent.split("\n");
        int count = 0;

        for (String line : lines) {
            // 只提取新增的代码行
            if (line.startsWith("+") && !line.startsWith("+++")) {
                snippets.append(line.substring(1)).append("\n");
                count++;
                // 限制提取的行数
                if (count >= 100) {
                    break;
                }
            }
        }

        return snippets.toString();
    }

    /**
     * 解析diff内容，按文件分组
     */
    private Map<String, List<String>> parseDiffByFile(String diffContent) {
        Map<String, List<String>> fileChanges = new HashMap<>();
        String currentFile = null;
        List<String> currentChanges = new ArrayList<>();
        int lineNumber = 0;

        for (String line : diffContent.lines().toList()) {
            if (line.startsWith("diff --git")) {
                if (currentFile != null && !currentChanges.isEmpty()) {
                    fileChanges.put(currentFile, new ArrayList<>(currentChanges));
                }

                String[] parts = line.split(" ");
                if (parts.length >= 4) {
                    currentFile = parts[3].substring(2);
                    currentChanges.clear();
                    lineNumber = 0;
                }
            } else if (line.startsWith("@@")) {
                Pattern pattern = Pattern.compile("\\+([0-9]+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    lineNumber = Integer.parseInt(matcher.group(1));
                }
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                currentChanges.add(lineNumber + ":" + line.substring(1));
                lineNumber++;
            } else if (!line.startsWith("-")) {
                lineNumber++;
            }
        }

        if (currentFile != null && !currentChanges.isEmpty()) {
            fileChanges.put(currentFile, currentChanges);
        }

        return fileChanges;
    }

    /**
     * 检查基础逻辑问题
     */
    private List<ReviewCommentDTO> checkBasicLogicIssues(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) {
                continue;
            }

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];

            // 检查死循环风险
            if (code.contains("while(true)") || code.contains("while (true)")) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("存在死循环风险，确保有适当的退出条件")
                        .severity("warning")
                        .build());
            }

            // 检查递归调用
            if (code.matches(".*\\b(\\w+)\\s*\\(.*\\).*") &&
                    changes.stream().anyMatch(c -> c.contains("return " + code.trim()))) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("可能存在递归调用，请确保有终止条件")
                        .severity("warning")
                        .build());
            }

            // 检查空的catch块
            if (code.contains("catch") && code.contains("{}")) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("空的catch块，应至少记录异常信息")
                        .severity("error")
                        .build());
            }
        }

        return issues;
    }

    /**
     * 检查空指针风险
     */
    private List<ReviewCommentDTO> checkNullPointerRisks(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];

            // 检查直接调用可能为null的对象
            if (code.matches(".*\\b(\\w+)\\.\\w+\\(.*") && !code.contains("!= null") && !code.contains("== null")) {
                // 检查变量是否有null检查
                String varName = code.replaceAll(".*\\b(\\w+)\\..*", "$1");
                boolean hasNullCheck = changes.stream()
                        .anyMatch(c -> c.contains(varName + " != null") || c.contains(varName + " == null"));

                if (!hasNullCheck && !isKnownNonNull(varName)) {
                    issues.add(ReviewCommentDTO.builder()
                            .filePath(filePath)
                            .lineNumber(lineNumber)
                            .comment("可能的空指针风险，建议添加null检查: " + varName)
                            .severity("warning")
                            .build());
                }
            }

            // 检查Optional的不当使用
            if (code.contains(".get()") && code.contains("Optional")) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("直接调用Optional.get()可能抛出NoSuchElementException，建议使用orElse或ifPresent")
                        .severity("warning")
                        .build());
            }
        }

        return issues;
    }

    /**
     * 检查资源泄露
     */
    private List<ReviewCommentDTO> checkResourceLeaks(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        Set<String> openedResources = new HashSet<>();
        Set<String> closedResources = new HashSet<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) {
                continue;
            }

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];

            // 检查资源打开
            if (code.contains("new FileInputStream") || code.contains("new FileOutputStream") ||
                    code.contains("new BufferedReader") || code.contains("new BufferedWriter") ||
                    code.contains("openConnection") || code.contains("getConnection")) {

                // 提取变量名
                Pattern pattern = Pattern.compile("(\\w+)\\s*=\\s*new");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    openedResources.add(matcher.group(1));
                }

                // 检查是否使用try-with-resources
                if (!code.contains("try (") && !code.contains("try(")) {
                    issues.add(ReviewCommentDTO.builder()
                            .filePath(filePath)
                            .lineNumber(lineNumber)
                            .comment("资源未使用try-with-resources，可能存在资源泄露")
                            .severity("warning")
                            .build());
                }
            }

            // 检查资源关闭
            if (code.contains(".close()")) {
                Pattern pattern = Pattern.compile("(\\w+)\\.close\\(\\)");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    closedResources.add(matcher.group(1));
                }
            }
        }

        // 检查未关闭的资源
        openedResources.removeAll(closedResources);
        for (String resource : openedResources) {
            issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(0)
                    // 无法确定具体行号
                    .comment("资源可能未正确关闭: " + resource)
                    .severity("error")
                    .build());
        }

        return issues;
    }

    /**
     * 使用LLM结合项目上下文进行深度分析
     */
    private List<ReviewCommentDTO> performContextualAnalysis(String filePath, List<String> changes,
                                                             String projectContext) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        try {
            // 构建代码片段
            StringBuilder codeSnippet = new StringBuilder();
            for (String change : changes.subList(0, Math.min(changes.size(), 30))) {
                codeSnippet.append(change).append("\n");
            }

            String prompt = String.format(
                    """
                            作为代码逻辑审查专家，基于项目上下文分析以下代码变更的逻辑问题:
                            
                            项目上下文:
                            %s
                            
                            文件: %s
                            代码变更:
                            %s
                            
                            请重点检查:
                            1. 代码逻辑是否合理
                            2. 是否与项目现有设计模式一致
                            3. 是否有潜在的性能问题
                            4. 是否影响现有功能
                            只列出最重要的3个问题，以JSON格式返回""",
                    projectContext != null ? projectContext.substring(0, Math.min(projectContext.length(), 1000)) : "无",
                    filePath, codeSnippet.toString()
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            logger.debug("LLM逻辑分析响应: {}", response);

        } catch (Exception e) {
            logger.error("LLM逻辑分析失败: {}", filePath, e);
        }

        return issues;
    }

    /**
     * 检查设计模式一致性
     */
    private List<ReviewCommentDTO> checkDesignPatternConsistency(String diffContent, String projectContext) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        if (projectContext == null || projectContext.isEmpty()) {
            return issues;
        }

        try {
            String prompt = String.format(
                    """
                            基于项目上下文，检查代码变更是否符合项目的设计模式:
                            
                            项目上下文:
                            %s
                            
                            代码变更摘要:
                            %s
                            
                            如果发现设计模式不一致的地方，请指出（最多2个）""",
                    projectContext.substring(0, Math.min(projectContext.length(), 500)),
                    diffContent.substring(0, Math.min(diffContent.length(), 500))
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 简单处理响应
            if (response != null && (response.contains("不一致") || response.contains("违反") || response.contains("不符合"))) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath("general")
                        .lineNumber(0)
                        .comment("设计模式问题: " + response.substring(0, Math.min(response.length(), 200)))
                        .severity("warning")
                        .build());
            }

        } catch (Exception e) {
            logger.error("设计模式一致性检查失败", e);
        }

        return issues;
    }

    /**
     * 检查是否有重复实现
     */
    private List<ReviewCommentDTO> checkDuplicateImplementation(String diffContent, String projectContext) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        if (projectContext == null || projectContext.isEmpty()) {
            return issues;
        }

        try {
            // 提取新增的方法签名
            List<String> newMethods = extractMethodSignatures(diffContent);

            if (!newMethods.isEmpty()) {
                String prompt = String.format(
                        """
                                基于项目上下文，检查以下新增方法是否已有类似实现:
                                
                                项目上下文:
                                %s
                                
                                新增方法:
                                %s
                                
                                如果发现重复或类似的实现，请指出""",
                        projectContext.substring(0, Math.min(projectContext.length(), 500)),
                        String.join("\n", newMethods)
                );

                String response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                if (response != null && (response.contains("重复") || response.contains("类似") || response.contains("已存在"))) {
                    issues.add(ReviewCommentDTO.builder()
                            .filePath("general")
                            .lineNumber(0)
                            .comment("可能的重复实现: " + response.substring(0, Math.min(response.length(), 200)))
                            .severity("info")
                            .build());
                }
            }

        } catch (Exception e) {
            logger.error("重复实现检查失败", e);
        }

        return issues;
    }

    /**
     * 提取方法签名
     */
    private List<String> extractMethodSignatures(String diffContent) {
        List<String> signatures = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\+\\s*(public|private|protected)\\s+[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\([^)]*\\)");
        Matcher matcher = pattern.matcher(diffContent);
        
        while (matcher.find() && signatures.size() < 10) {
            signatures.add(matcher.group());
        }

        return signatures;
    }

    /**
     * 判断变量是否已知非null
     */
    private boolean isKnownNonNull(String varName) {
        // 一些已知不会为null的变量名模式
        return "this".equals(varName) ||
                "logger".equals(varName) ||
                varName.startsWith("LOG") ||
                varName.endsWith("Logger") ||
                "System".equals(varName);
    }
}