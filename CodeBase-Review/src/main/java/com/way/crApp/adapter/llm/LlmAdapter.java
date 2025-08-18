package com.way.crApp.adapter.llm;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.way.crApp.dto.review.ReviewResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * LLM 适配器
 * 
 * 职责：封装与 Spring AI ChatClient 的交互细节
 * 核心方法：getReviewComments(String diff, String styleContext, String projectContext)
 * 内部负责构建 PromptTemplate，设置 BeanOutputParser，并调用 chatClient.call()
 */
@Component
public class LlmAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LlmAdapter.class);

    private final ChatClient chatClient;

    public LlmAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .withTopP(0.7)
                                .build()
                )
                .build();
    }

    /**
     * 获取代码审查意见
     * 
     * @param diffContent diff内容
     * @param contextInfo RAG检索到的上下文信息
     * @param repoFullName 仓库全名
     * @return 审查结果
     */
    public ReviewResultDTO getReviewComments(String diffContent, String contextInfo, String repoFullName) {
        try {
            logger.info("开始调用LLM进行代码审查: repo={}", repoFullName);
            
            // 创建输出转换器
            BeanOutputConverter<ReviewResultDTO> outputConverter = new BeanOutputConverter<>(ReviewResultDTO.class);
            
            // 构建审查提示词
            String prompt = buildReviewPrompt(diffContent, contextInfo, repoFullName);
            logger.info("LLM审查提示词: repo={}, prompt={}", repoFullName, prompt);
            // 调用ChatClient获取结构化输出
            ReviewResultDTO result = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ReviewResultDTO.class);
            
            logger.info("LLM审查完成: repo={}, 生成评论数={}", 
                repoFullName, result != null && result.comments() != null ? result.comments().size() : 0);
            logger.info("LLM审查结果: repo={}, result={}", repoFullName, result.toString());
            return result;
            
        } catch (Exception e) {
            logger.error("调用LLM时发生错误: repo={}", repoFullName, e);
            return null;
        }
    }

    /**
     * 构建代码审查提示词
     * 
     * @param diffContent diff内容
     * @param contextInfo 上下文信息
     * @param repoFullName 仓库名
     * @return 完整的提示词
     */
    private String buildReviewPrompt(String diffContent, String contextInfo, String repoFullName) {
        return String.format("""
            你是一个专业的Java代码审查专家。请基于以下信息对代码变更进行详细审查：
            
            **项目信息:**
            - 仓库: %s
            
            **相关规范和上下文:**
            %s
            
            **代码变更 (Diff):**
            ```diff
            %s
            ```
            
            **审查要求:**
            1. 重点关注代码质量、性能、安全性和最佳实践
            2. 参考提供的规范和项目上下文
            3. 对于每个问题，提供具体的文件路径和行号
            4. 评论应该具体、可操作，避免泛泛而谈
            5. 如果代码质量良好，可以给出积极的反馈
            6. 严重程度分为: info, warning, error
            
            **输出格式:**
            请返回JSON格式的审查结果，包含以下字段：
            - comments: 评论列表，每条评论包含 file_path, line_number, comment, severity
            - summary: 整体评审总结
            - overall_rating: 整体评级 (excellent/good/needs_improvement/poor)
            
            示例格式：
            {
              "comments": [
                {
                  "file_path": "src/main/java/Example.java",
                  "line_number": 15,
                  "comment": "建议使用StringBuilder代替字符串拼接以提高性能",
                  "severity": "warning"
                }
              ],
              "summary": "代码整体质量良好，有少量可优化的地方",
              "overall_rating": "good"
            }
            """, repoFullName, contextInfo != null ? contextInfo : "暂无相关上下文", diffContent);
    }
} 