package com.hxg.crApp.config;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.hxg.crApp.agent.*;
import com.hxg.crApp.graph.ReviewDecisionRouter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.*;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 多智能体代码审查工作流配置
 * 
 * 工作流结构：
 * 1. START -> ReviewCoordinatorAgent -> TriageAgent
 * 2. TriageAgent 条件分支：
 *    - 需要详细审查 -> ParallelReviewStarter -> 并行执行三个专家Agent
 *    - 跳过详细审查 -> 直接到ReportSynthesizerAgent
 *    - PR过大/无效 -> 直接结束
 * 3. 三个专家Agent并行执行：StyleConventionAgent, LogicContextAgent, SecurityScanAgent
 * 4. 专家Agent完成 -> ReportSynthesizerAgent -> END
 * @author hxg
 */
@Configuration
public class CodeReviewWorkflowConfig {
    
    /**
     * 创建代码审查工作流的StateGraph
     */
    @Bean
    public StateGraph codeReviewWorkflow(
            ChatModel chatModel,
            ReviewCoordinatorAgent coordinatorAgent,
            TriageAgent triageAgent,
            ParallelReviewStarter parallelStarter,
            StyleConventionAgent styleAgent,
            LogicContextAgent logicAgent,
            SecurityScanAgent securityAgent,
            ReportSynthesizerAgent reportAgent,
            ReviewDecisionRouter decisionRouter) throws GraphStateException {
        
        // 创建ChatClient供Agent使用
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        
        // 定义状态管理策略
        KeyStrategyFactory stateFactory = createStateFactory();
        
        // 构建工作流
        return new StateGraph("代码审查多智能体工作流", stateFactory)
            
            // 添加所有节点
            .addNode("coordinator", node_async(coordinatorAgent))
            .addNode("triage", node_async(triageAgent))
            .addNode("parallel_starter", node_async(parallelStarter))
            .addNode("style_review", node_async(styleAgent))
            .addNode("logic_review", node_async(logicAgent))
            .addNode("security_review", node_async(securityAgent))
            .addNode("report_synthesizer", node_async(reportAgent))
            
            // 定义工作流路径
            .addEdge(START, "coordinator")
            .addEdge("coordinator", "triage")
            
            // 条件分支：根据triage结果决定是否进行详细审查
            .addConditionalEdges("triage", 
                edge_async(decisionRouter),
                Map.of(
                    "detailed_review", "parallel_starter",   // 进行详细审查，先启动并行启动器
                    "skip_review", "report_synthesizer",     // 跳过详细审查
                    "too_large", END,                        // PR过大，直接结束
                    "invalid", END                           // PR无效，直接结束
                ))
            
            // 并行审查：从parallel_starter触发三个专家Agent
            .addEdge("parallel_starter", "style_review")
            .addEdge("parallel_starter", "logic_review")
            .addEdge("parallel_starter", "security_review")
            
            // 所有专家Agent完成后汇集到报告生成
            .addEdge("style_review", "report_synthesizer")
            .addEdge("logic_review", "report_synthesizer") 
            .addEdge("security_review", "report_synthesizer")
            
            // 结束流程
            .addEdge("report_synthesizer", END);
    }
    
    /**
     * 创建状态管理策略工厂
     */
    private KeyStrategyFactory createStateFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            
            // 基础状态 - 替换策略
            strategies.put("review_task", new ReplaceStrategy());
            strategies.put("diff_content", new ReplaceStrategy());
            strategies.put("repo_name", new ReplaceStrategy());
            strategies.put("pr_number", new ReplaceStrategy());
            strategies.put("pr_title", new ReplaceStrategy());
            strategies.put("pr_author", new ReplaceStrategy());
            strategies.put("changed_files", new ReplaceStrategy());
            strategies.put("diff_stats", new ReplaceStrategy());
            strategies.put("review_config", new ReplaceStrategy());
            
            // Triage阶段状态
            strategies.put("triage_decision", new ReplaceStrategy());
            strategies.put("triage_message", new ReplaceStrategy());
            strategies.put("needs_detailed_review", new ReplaceStrategy());
            strategies.put("change_type", new ReplaceStrategy());
            strategies.put("review_scope", new ReplaceStrategy());
            strategies.put("pr_intent", new ReplaceStrategy());
            
            // 并行审查启动状态
            strategies.put("parallel_review_started", new ReplaceStrategy());
            strategies.put("parallel_review_status", new ReplaceStrategy());
            
            // 审查结果 - 追加策略（支持合并多个Agent的结果）
            strategies.put("style_issues", new AppendStrategy());
            strategies.put("logic_issues", new AppendStrategy());
            strategies.put("security_issues", new AppendStrategy());
            
            // 统计信息 - 替换策略
            strategies.put("style_issues_count", new ReplaceStrategy());
            strategies.put("logic_issues_count", new ReplaceStrategy());
            strategies.put("security_issues_count", new ReplaceStrategy());
            strategies.put("style_error_count", new ReplaceStrategy());
            strategies.put("logic_error_count", new ReplaceStrategy());
            strategies.put("critical_security_count", new ReplaceStrategy());
            strategies.put("security_risk_level", new ReplaceStrategy());
            
            // Memory服务相关
            strategies.put("has_project_context", new ReplaceStrategy());
            
            // 最终结果
            strategies.put("final_review_result", new ReplaceStrategy());
            strategies.put("markdown_report", new ReplaceStrategy());
            strategies.put("issue_statistics", new ReplaceStrategy());
            strategies.put("total_issues", new ReplaceStrategy());
            
            return strategies;
        };
    }
    
    /**
     * 编译工作流为可执行图
     */
    @Bean 
    public com.alibaba.cloud.ai.graph.CompiledGraph compiledCodeReviewGraph(StateGraph codeReviewWorkflow) throws GraphStateException {
        return codeReviewWorkflow.compile();
    }
}