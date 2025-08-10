package com.hxg.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.hxg.crApp.dto.review.ReviewCommentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 编码规范审查员Agent - 基于内置规范知识库检查代码规范
 * <p>
 * 职责：
 * 1. 检查命名规范（类名、方法名、变量名）
 * 2. 检查代码格式化和缩进
 * 3. 检查注释规范
 * 4. 检查是否使用了不推荐的API
 * 5. 基于阿里巴巴Java开发手册等规范进行审查
 * @author hxg
 */
@Component
public class StyleConventionAgent implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(StyleConventionAgent.class);

    @Autowired
    private ChatClient chatClient;

    // 规范规则定义
    private static final Map<String, String> NAMING_RULES = new HashMap<>();
    private static final List<String> DEPRECATED_APIS = new ArrayList<>();
    private static final Map<String, String> CODE_SMELLS = new HashMap<>();

    static {
        // 初始化命名规范
        NAMING_RULES.put("class", "^[A-Z][a-zA-Z0-9]*$");
        NAMING_RULES.put("method", "^[a-z][a-zA-Z0-9]*$");
        NAMING_RULES.put("constant", "^[A-Z][A-Z0-9_]*$");
        NAMING_RULES.put("variable", "^[a-z][a-zA-Z0-9]*$");
        NAMING_RULES.put("package", "^[a-z]+(\\.[a-z]+)*$");

        // 不推荐的API
        // 应使用 LocalDateTime
        DEPRECATED_APIS.add("Date");
        // 应使用 LocalDate
        DEPRECATED_APIS.add("Calendar");
        // 应使用 DateTimeFormatter
        DEPRECATED_APIS.add("SimpleDateFormat");
        // 应使用 ArrayList
        DEPRECATED_APIS.add("Vector");
        // 应使用 HashMap
        DEPRECATED_APIS.add("Hashtable"); 

        // 代码坏味道模式
        CODE_SMELLS.put("System.out.println", "不应使用System.out，应使用日志框架");
        CODE_SMELLS.put("printStackTrace\\(\\)", "不应使用printStackTrace，应使用日志记录异常");
        CODE_SMELLS.put("throw new Exception", "不应抛出通用Exception，应使用具体异常类型");
        CODE_SMELLS.put("catch\\s*\\(\\s*Exception", "不应捕获通用Exception，应捕获具体异常");
        CODE_SMELLS.put("// TODO", "存在未完成的TODO");
        CODE_SMELLS.put("// FIXME", "存在需要修复的FIXME");
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("编码规范审查员Agent开始执行");

        String diffContent = (String) state.value("diff_content").orElse("");
        String[] changedFiles = (String[]) state.value("changed_files").orElse(new String[0]);

        List<ReviewCommentDTO> styleIssues = new ArrayList<>();

        // 分析每个文件的diff
        Map<String, List<String>> fileChanges = parseDiffByFile(diffContent);

        for (Map.Entry<String, List<String>> entry : fileChanges.entrySet()) {
            String filePath = entry.getKey();
            List<String> changes = entry.getValue();

            // 只检查Java文件
            if (!filePath.endsWith(".java")) {
                continue;
            }

            logger.debug("检查文件规范: {}", filePath);

            // 1. 检查命名规范
            styleIssues.addAll(checkNamingConventions(filePath, changes));

            // 2. 检查不推荐的API使用
            styleIssues.addAll(checkDeprecatedApis(filePath, changes));

            // 3. 检查代码坏味道
            styleIssues.addAll(checkCodeSmells(filePath, changes));

            // 4. 检查注释规范
            styleIssues.addAll(checkCommentConventions(filePath, changes));

            // 5. 使用LLM进行更深入的规范检查
            if (changes.size() > 0) {
                styleIssues.addAll(performLlmStyleCheck(filePath, changes));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("style_issues", styleIssues);
        result.put("style_issues_count", styleIssues.size());

        // 按严重程度分类统计
        long errors = styleIssues.stream().filter(i -> "error".equals(i.severity())).count();
        long warnings = styleIssues.stream().filter(i -> "warning".equals(i.severity())).count();
        long infos = styleIssues.stream().filter(i -> "info".equals(i.severity())).count();

        result.put("style_error_count", errors);
        result.put("style_warning_count", warnings);
        result.put("style_info_count", infos);

        logger.info("编码规范检查完成，发现问题: {} (错误:{}, 警告:{}, 信息:{})",
                styleIssues.size(), errors, warnings, infos);

        return result;
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
                // 保存上一个文件的变更
                if (currentFile != null && !currentChanges.isEmpty()) {
                    fileChanges.put(currentFile, new ArrayList<>(currentChanges));
                }

                // 解析新文件路径
                String[] parts = line.split(" ");
                if (parts.length >= 4) {
                    // 去掉 b/ 前缀
                    currentFile = parts[3].substring(2);
                    currentChanges.clear();
                    lineNumber = 0;
                }
            } else if (line.startsWith("@@")) {
                // 解析行号信息
                Pattern pattern = Pattern.compile("\\+([0-9]+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    lineNumber = Integer.parseInt(matcher.group(1));
                }
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                // 记录新增的行
                currentChanges.add(lineNumber + ":" + line.substring(1));
                lineNumber++;
            } else if (!line.startsWith("-")) {
                lineNumber++;
            }
        }

        // 保存最后一个文件
        if (currentFile != null && !currentChanges.isEmpty()) {
            fileChanges.put(currentFile, currentChanges);
        }

        return fileChanges;
    }

    /**
     * 检查命名规范
     */
    private List<ReviewCommentDTO> checkNamingConventions(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) {
                continue;
            }

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];

            // 检查类名
            if (code.contains("class ")) {
                Pattern pattern = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    String className = matcher.group(1);
                    if (!className.matches(NAMING_RULES.get("class"))) {
                        issues.add(ReviewCommentDTO.builder()
                                .filePath(filePath)
                                .lineNumber(lineNumber)
                                .comment("类名不符合规范，应以大写字母开头，使用驼峰命名: " + className)
                                .severity("warning")
                                .build());
                    }
                }
            }

            // 检查方法名
            if (code.contains("public ") || code.contains("private ") || code.contains("protected ")) {
                Pattern pattern = Pattern.compile("\\b(public|private|protected)\\s+[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\(");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    String methodName = matcher.group(2);
                    if (!methodName.matches(NAMING_RULES.get("method"))) {
                        issues.add(ReviewCommentDTO.builder()
                                .filePath(filePath)
                                .lineNumber(lineNumber)
                                .comment("方法名不符合规范，应以小写字母开头，使用驼峰命名: " + methodName)
                                .severity("warning")
                                .build());
                    }
                }
            }

            // 检查常量
            if (code.contains("static final")) {
                Pattern pattern = Pattern.compile("static\\s+final\\s+[\\w<>\\[\\]]+\\s+(\\w+)");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    String constantName = matcher.group(1);
                    if (!constantName.matches(NAMING_RULES.get("constant"))) {
                        issues.add(ReviewCommentDTO.builder()
                                .filePath(filePath)
                                .lineNumber(lineNumber)
                                .comment("常量名不符合规范，应全部大写，用下划线分隔: " + constantName)
                                .severity("warning")
                                .build());
                    }
                }
            }
        }

        return issues;
    }

    /**
     * 检查不推荐的API使用
     */
    private List<ReviewCommentDTO> checkDeprecatedApis(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];

            for (String api : DEPRECATED_APIS) {
                if (code.contains(api)) {
                    String suggestion = getApiModernReplacement(api);
                    issues.add(ReviewCommentDTO.builder()
                            .filePath(filePath)
                            .lineNumber(lineNumber)
                            .comment("不推荐使用 " + api + "，" + suggestion)
                            .severity("warning")
                            .build());
                }
            }
        }

        return issues;
    }

    /**
     * 检查代码坏味道
     */
    private List<ReviewCommentDTO> checkCodeSmells(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) {
                continue;
            }

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];

            for (Map.Entry<String, String> smell : CODE_SMELLS.entrySet()) {
                Pattern pattern = Pattern.compile(smell.getKey());
                if (pattern.matcher(code).find()) {
                    issues.add(ReviewCommentDTO.builder()
                            .filePath(filePath)
                            .lineNumber(lineNumber)
                            .comment(smell.getValue())
                            .severity("info")
                            .build());
                }
            }
        }

        return issues;
    }

    /**
     * 检查注释规范
     */
    private List<ReviewCommentDTO> checkCommentConventions(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) {
                continue;
            }

            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1].trim();

            // 检查类和公共方法是否有JavaDoc
            if ((code.startsWith("public class") || code.startsWith("public interface") ||
                    (code.startsWith("public ") && code.contains("(") && !code.contains("{")))
                    && !isPrecededByJavaDoc(changes, change)) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("公共类或方法缺少JavaDoc注释")
                        .severity("info")
                        .build());
            }

            // 检查注释是否有意义
            if (code.contains("//") &&
                    (code.contains("// 获取") || code.contains("// 设置") ||
                            code.contains("// 返回") || code.contains("// 参数"))) {
                issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("注释过于简单，应提供更有意义的说明")
                        .severity("info")
                        .build());
            }
        }

        return issues;
    }

    /**
     * 使用LLM进行更深入的规范检查
     */
    private List<ReviewCommentDTO> performLlmStyleCheck(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();

        try {
            // 构建代码片段
            StringBuilder codeSnippet = new StringBuilder();
            for (String change : changes.subList(0, Math.min(changes.size(), 50))) { // 限制50行
                codeSnippet.append(change).append("\n");
            }

            String prompt = String.format(
                    "作为Java代码规范审查专家，请基于阿里巴巴Java开发手册检查以下代码，只列出最重要的3个问题:\n" +
                            "文件: %s\n" +
                            "代码:\n%s\n" +
                            "请以JSON格式返回，格式: [{\"line\": 行号, \"issue\": \"问题描述\", \"severity\": \"warning\"}]",
                    filePath, codeSnippet.toString()
            );

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 解析LLM返回的问题（简化处理）
            // 实际应该使用JSON解析器
            logger.debug("LLM规范检查响应: {}", response);

        } catch (Exception e) {
            logger.error("LLM规范检查失败: {}", filePath, e);
        }

        return issues;
    }

    /**
     * 获取API的现代替代方案
     */
    private String getApiModernReplacement(String api) {
        return switch (api) {
            case "Date" -> "建议使用 java.time.LocalDateTime";
            case "Calendar" -> "建议使用 java.time.LocalDate";
            case "SimpleDateFormat" -> "建议使用 java.time.format.DateTimeFormatter";
            case "Vector" -> "建议使用 ArrayList";
            case "Hashtable" -> "建议使用 HashMap 或 ConcurrentHashMap";
            default -> "建议使用更现代的API";
        };
    }

    /**
     * 检查是否有JavaDoc注释
     */
    private boolean isPrecededByJavaDoc(List<String> changes, String currentChange) {
        int currentIndex = changes.indexOf(currentChange);
        if (currentIndex > 0) {
            String previousLine = changes.get(currentIndex - 1);
            return previousLine.contains("*/") || previousLine.contains("*");
        }
        return false;
    }
}