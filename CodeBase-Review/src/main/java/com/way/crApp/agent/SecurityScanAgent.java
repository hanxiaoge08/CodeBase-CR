package com.way.crApp.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.way.crApp.dto.review.ReviewCommentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 安全漏洞扫描员Agent - 基于安全规则库检测潜在安全风险
 * 
 * 职责：
 * 1. 检查常见注入风险（SQL注入、XSS等）
 * 2. 检查硬编码密钥或密码
 * 3. 检查不安全的加密算法
 * 4. 检查不当的异常处理可能导致信息泄露
 * 5. 基于OWASP Top 10安全规则进行扫描
 */
@Component
public class SecurityScanAgent implements NodeAction {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityScanAgent.class);
    
    @Autowired
    private ChatClient chatClient;
    
    // SQL注入风险模式
    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
        Pattern.compile("\"[^\"]*\\+.*\\+[^\"]*\""), // 字符串拼接SQL
        Pattern.compile("\\$\\{.*\\}"), // 可能的SQL参数注入
        Pattern.compile("PreparedStatement.*\\+"), // PreparedStatement错误使用
        Pattern.compile("Statement.*executeQuery\\(.*\\+"), // Statement拼接
        Pattern.compile("createQuery\\(.*\\+") // JPA Query拼接
    );
    
    // XSS风险模式
    private static final List<Pattern> XSS_PATTERNS = List.of(
        Pattern.compile("response\\.getWriter\\(\\)\\.print.*\\+"), // 直接输出用户输入
        Pattern.compile("out\\.print.*\\+.*request\\.getParameter"), // JSP直接输出
        Pattern.compile("innerHTML.*\\+"), // JavaScript innerHTML拼接
        Pattern.compile("document\\.write.*\\+") // JavaScript document.write
    );
    
    // 硬编码密钥模式
    private static final List<Pattern> HARDCODED_SECRET_PATTERNS = List.of(
        Pattern.compile("(password|passwd|pwd)\\s*=\\s*[\"'][^\"']{3,}[\"']", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(secret|key|token)\\s*=\\s*[\"'][^\"']{10,}[\"']", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(api[_-]?key|access[_-]?token)\\s*=\\s*[\"'][^\"']+[\"']", Pattern.CASE_INSENSITIVE),
        Pattern.compile("[\"'][A-Za-z0-9]{20,}[\"']") // 长字符串可能是密钥
    );
    
    // 不安全加密算法
    private static final List<String> INSECURE_ALGORITHMS = List.of(
        "MD5", "SHA1", "DES", "RC4", "MD2"
    );
    
    // 不安全的随机数生成
    private static final List<String> INSECURE_RANDOM = List.of(
        "Math.random()", "Random()", "new Random("
    );
    
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("安全漏洞扫描员Agent开始执行");
        
        String diffContent = (String) state.value("diff_content").orElse("");
        String[] changedFiles = (String[]) state.value("changed_files").orElse(new String[0]);
        
        List<ReviewCommentDTO> securityIssues = new ArrayList<>();
        
        // 解析diff内容
        Map<String, List<String>> fileChanges = parseDiffByFile(diffContent);
        
        for (Map.Entry<String, List<String>> entry : fileChanges.entrySet()) {
            String filePath = entry.getKey();
            List<String> changes = entry.getValue();
            
            // 只检查代码文件
            if (!isCodeFile(filePath)) {
                continue;
            }
            
            logger.debug("安全扫描文件: {}", filePath);
            
            // 1. SQL注入风险检查
            securityIssues.addAll(checkSQLInjectionRisks(filePath, changes));
            
            // 2. XSS风险检查
            securityIssues.addAll(checkXSSRisks(filePath, changes));
            
            // 3. 硬编码密钥检查
            securityIssues.addAll(checkHardcodedSecrets(filePath, changes));
            
            // 4. 不安全加密算法检查
            securityIssues.addAll(checkInsecureCrypto(filePath, changes));
            
            // 5. 权限和认证问题检查
            securityIssues.addAll(checkAuthenticationIssues(filePath, changes));
            
            // 6. 文件操作安全检查
            securityIssues.addAll(checkFileOperationSecurity(filePath, changes));
            
            // 7. 反序列化安全检查
            securityIssues.addAll(checkDeserializationSecurity(filePath, changes));
        }
        
        // 8. 使用LLM进行高级安全分析
        securityIssues.addAll(performAdvancedSecurityAnalysis(diffContent));
        
        Map<String, Object> result = new HashMap<>();
        result.put("security_issues", securityIssues);
        result.put("security_issues_count", securityIssues.size());
        
        // 按严重程度分类统计
        long critical = securityIssues.stream().filter(i -> "error".equals(i.severity())).count();
        long high = securityIssues.stream().filter(i -> "warning".equals(i.severity())).count();
        long medium = securityIssues.stream().filter(i -> "info".equals(i.severity())).count();
        
        result.put("critical_security_count", critical);
        result.put("high_security_count", high);
        result.put("medium_security_count", medium);
        
        // 计算安全风险等级
        String riskLevel = calculateSecurityRiskLevel(critical, high, medium);
        result.put("security_risk_level", riskLevel);
        
        logger.info("安全扫描完成，发现问题: {} (严重:{}, 高危:{}, 中等:{}), 风险等级: {}", 
            securityIssues.size(), critical, high, medium, riskLevel);
        
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
     * 检查是否是代码文件
     */
    private boolean isCodeFile(String filePath) {
        return filePath.endsWith(".java") || filePath.endsWith(".js") || 
               filePath.endsWith(".jsp") || filePath.endsWith(".html") ||
               filePath.endsWith(".xml") || filePath.endsWith(".properties");
    }
    
    /**
     * 检查SQL注入风险
     */
    private List<ReviewCommentDTO> checkSQLInjectionRisks(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            for (Pattern pattern : SQL_INJECTION_PATTERNS) {
                if (pattern.matcher(code).find()) {
                    issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("潜在SQL注入风险：避免字符串拼接构建SQL，使用参数化查询")
                        .severity("error")
                        .build());
                    break; // 避免重复报告同一行
                }
            }
            
            // 检查动态SQL构建
            if (code.contains("StringBuilder") && code.contains("sql")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("动态SQL构建存在注入风险，建议使用MyBatis动态SQL或参数化查询")
                    .severity("warning")
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * 检查XSS风险
     */
    private List<ReviewCommentDTO> checkXSSRisks(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            for (Pattern pattern : XSS_PATTERNS) {
                if (pattern.matcher(code).find()) {
                    issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("潜在XSS风险：直接输出用户输入，需要进行HTML转义")
                        .severity("error")
                        .build());
                    break;
                }
            }
            
            // 检查JSTL输出
            if (code.contains("${") && !code.contains("fn:escapeXml")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("JSTL表达式可能存在XSS风险，建议使用fn:escapeXml进行转义")
                    .severity("warning")
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * 检查硬编码密钥
     */
    private List<ReviewCommentDTO> checkHardcodedSecrets(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            for (Pattern pattern : HARDCODED_SECRET_PATTERNS) {
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    String matched = matcher.group();
                    // 排除明显的示例值
                    if (!isExampleValue(matched)) {
                        issues.add(ReviewCommentDTO.builder()
                            .filePath(filePath)
                            .lineNumber(lineNumber)
                            .comment("疑似硬编码密钥或密码，建议使用配置文件或环境变量")
                            .severity("error")
                            .build());
                    }
                }
            }
        }
        
        return issues;
    }
    
    /**
     * 检查不安全加密算法
     */
    private List<ReviewCommentDTO> checkInsecureCrypto(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            for (String algorithm : INSECURE_ALGORITHMS) {
                if (code.contains(algorithm)) {
                    issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment(String.format("使用了不安全的加密算法: %s，建议使用SHA-256或更强的算法", algorithm))
                        .severity("warning")
                        .build());
                }
            }
            
            // 检查不安全的随机数生成
            for (String random : INSECURE_RANDOM) {
                if (code.contains(random)) {
                    issues.add(ReviewCommentDTO.builder()
                        .filePath(filePath)
                        .lineNumber(lineNumber)
                        .comment("使用了不安全的随机数生成器，安全敏感场景应使用SecureRandom")
                        .severity("info")
                        .build());
                }
            }
        }
        
        return issues;
    }
    
    /**
     * 检查权限和认证问题
     */
    private List<ReviewCommentDTO> checkAuthenticationIssues(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            // 检查跳过认证
            if (code.contains("permitAll()") || code.contains("anonymous()")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("跳过认证的接口，确认是否确实需要公开访问")
                    .severity("info")
                    .build());
            }
            
            // 检查硬编码的用户信息
            if (code.matches(".*['\"][a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}['\"].*")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("硬编码的邮箱地址，可能是测试代码残留")
                    .severity("info")
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * 检查文件操作安全
     */
    private List<ReviewCommentDTO> checkFileOperationSecurity(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            // 检查路径遍历风险
            if (code.contains("..") && (code.contains("File") || code.contains("Path"))) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("可能的路径遍历攻击风险，需要验证文件路径")
                    .severity("error")
                    .build());
            }
            
            // 检查文件上传安全
            if (code.contains("MultipartFile") && !code.contains("getOriginalFilename")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("文件上传需要验证文件类型和大小，防止恶意文件上传")
                    .severity("warning")
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * 检查反序列化安全
     */
    private List<ReviewCommentDTO> checkDeserializationSecurity(String filePath, List<String> changes) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        for (String change : changes) {
            String[] parts = change.split(":", 2);
            if (parts.length < 2) continue;
            
            int lineNumber = Integer.parseInt(parts[0]);
            String code = parts[1];
            
            // 检查不安全的反序列化
            if (code.contains("ObjectInputStream") || code.contains("readObject")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("Java反序列化存在安全风险，避免反序列化不可信数据")
                    .severity("error")
                    .build());
            }
            
            // 检查FastJson
            if (code.contains("JSON.parseObject") || code.contains("JSON.parse")) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath(filePath)
                    .lineNumber(lineNumber)
                    .comment("FastJson反序列化需要注意版本安全漏洞，建议使用Jackson")
                    .severity("warning")
                    .build());
            }
        }
        
        return issues;
    }
    
    /**
     * 使用LLM进行高级安全分析
     */
    private List<ReviewCommentDTO> performAdvancedSecurityAnalysis(String diffContent) {
        List<ReviewCommentDTO> issues = new ArrayList<>();
        
        try {
            // 提取关键代码片段进行分析
            String codeSnippet = diffContent.lines()
                .filter(line -> line.startsWith("+") && !line.startsWith("+++"))
                .limit(100)
                .reduce("", (a, b) -> a + "\n" + b.substring(1));
            
            if (codeSnippet.trim().isEmpty()) {
                return issues;
            }
            
            String prompt = String.format(
                "作为安全专家，请分析以下代码变更的安全风险，重点关注OWASP Top 10:\n\n" +
                "%s\n\n" +
                "请列出最严重的3个安全问题（如果存在），格式：\n" +
                "1. [风险类型] 具体问题描述\n" +
                "如果没有明显安全问题，返回：无明显安全风险",
                codeSnippet
            );
            
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            
            // 简单解析LLM响应
            if (!response.contains("无明显安全风险") && !response.trim().isEmpty()) {
                issues.add(ReviewCommentDTO.builder()
                    .filePath("general")
                    .lineNumber(0)
                    .comment("LLM安全分析: " + response.substring(0, Math.min(response.length(), 300)))
                    .severity("info")
                    .build());
            }
            
        } catch (Exception e) {
            logger.error("LLM安全分析失败", e);
        }
        
        return issues;
    }
    
    /**
     * 判断是否是示例值
     */
    private boolean isExampleValue(String value) {
        String lower = value.toLowerCase();
        return lower.contains("example") || lower.contains("test") || 
               lower.contains("demo") || lower.contains("123456") ||
               lower.contains("password") || lower.equals("\"\"") ||
               lower.equals("''") || lower.contains("your_");
    }
    
    /**
     * 计算安全风险等级
     */
    private String calculateSecurityRiskLevel(long critical, long high, long medium) {
        if (critical > 0) {
            return "CRITICAL";
        } else if (high >= 3) {
            return "HIGH";
        } else if (high > 0 || medium >= 5) {
            return "MEDIUM";
        } else if (medium > 0) {
            return "LOW";
        } else {
            return "SAFE";
        }
    }
}