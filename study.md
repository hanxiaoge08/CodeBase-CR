下面把这套 **1+4+1 多智能体工作流**按“数据怎么流、谁在什么时候做什么、失败怎么兜底”的思路完整讲一遍。你拿着这份就能从 Webhook 进来一路追到报告产出。

---

# 0. 入口与总体时序

**触发**：CI/CD 或 Git 平台 Webhook（如 PR 创建/更新）。
**核心对象**：`OverAllState`（全局状态容器）。
**总流程**：

1. `START`：初始化 `OverAllState`，写入

    * `review_task`（`ReviewTaskDTO`：repo、PR号、标题、作者、base/head 等）
    * `diff_content`（统一 diff 文本，或变更文件+patch）

2. `ReviewCoordinatorAgent`（协调员 / 入口解析）

3. `TriageAgent`（初筛和路由决策）

4. `ParallelStarter` 并发启动 3 个专家：

    * `StyleConventionAgent`（带 RAG）
    * `LogicContextAgent`（带 Memory）
    * `SecurityScanAgent`

5. `ReportSynthesizerAgent` 汇总

6. `END`：落库、评论 PR、推送报告

---

# 1. ReviewCoordinatorAgent（PR元数据解析，打基础）

**输入**（来自 `OverAllState`）：

* `review_task`、`diff_content`

**处理**：

* 解析 PR 维度：`repo_name`、`pr_number`、`pr_title`、`pr_author`、`base_sha`、`head_sha`
* 解析 diff：

    * `changed_files`：从 `diff --git a/... b/...` 提取目标文件（建议用正则更稳）
    * `diff_stats`：`lines_added`、`lines_deleted`、`files_changed`、`total_changes`
* 写入审查配置 `review_config`（如最大问题数、启用安全/性能检查等）

**输出**（回写到 `OverAllState`）：

* `repo_name`、`pr_*` 基础信息
* `changed_files`、`diff_stats`、`review_config`

> 作用：后续所有 Agent **都不再碰原始 diff**，直接消费“已解析的、结构化的上下文”。

---

# 2. TriageAgent（初筛与分流）

**输入**：

* `pr_title`、`diff_content`、`changed_files`、`diff_stats`（来自上一步）

**核心规则**：

* **体量拦截**：`total_changes > 1000` ⇒ `triage_decision = too_large`（建议拆分）
* **文件数拦截**：`files_changed > 20` ⇒ `too_many_files`
* **标题规范**：空/过短/过长 ⇒ `invalid_title`（提示 or 阻断，按你的策略）
* **变更类型识别**：

    * `code_only / documentation / configuration / test_only / mixed / other`
* **是否进入详细审查**：

    * 有代码/测试 ⇒ 详细审查
    * 文档/配置 ⇒ 通常跳过
    * 其他类型 ⇒ 视行数阈值

**可选**（你已实现）：

* 用 LLM 粗述 **PR意图**（diff 采样 + 标题），写入 `pr_intent`

**输出**：

* `triage_decision`：`proceed | skip_detailed | too_large | too_many_files | invalid_title`
* `triage_message`（给人看的提示）
* `needs_detailed_review`（bool）
* `change_type`、`review_scope`（如 `style/logic/security/performance/test_coverage`）
* `pr_intent`（可选）

> 作用：**决定是否继续**，以及**把下游要关注的范围**标签化，减少无效工作。

---

# 3. ParallelStarter（并行启动三个专家）

**触发条件**：`needs_detailed_review = true`。
**并行度**：同时启动以下 3 个 Agent，互不阻塞；每个 Agent 自行超时与降级。

## 3.1 StyleConventionAgent（编码规范 / 接入 RAG）

**输入**：

* `changed_files`、`diff_stats`、`review_scope`、`repo_name`、`pr_title`、`pr_intent`
* 代码片段/patch（按需获取）

**做什么**：

* 基于项目/公司“编码规范知识库”（**RAGService**）检索相关规范
* 执行风格/命名/格式/可读性检查（可用静态规则 + LLM 解释）
* 形成 `style_findings`（结构化问题：文件、行、规则ID、建议、证据片段）

**降级**：

* 知识库不可用 ⇒ 回退到内置规则集，产出简化建议

## 3.2 LogicContextAgent（逻辑审查 / 接入 Memory）

**输入**：

* 与 Style 类似 + `ReviewMemoryService` 能提供的“项目历史记忆”（类/模块演进、过往 Bug、已决设计）

**做什么**：

* 结合 Memory 的上下文（例如“这个函数之前因为X限制不能这么写”）
* 重点查：边界条件、并发、空指针、NPE、资源泄露、算法复杂度、兼容性
* 形成 `logic_findings`

**降级**：

* Memory 不可用 ⇒ 仅基于当前 diff 推断，跳过跨版本约束

## 3.3 SecurityScanAgent（安全检测）

**输入**：

* 同上

**做什么**：

* 静态检测（SAST 规则）+ 依赖漏洞（可选）+ LLM 审阅高危片段
* 关注：注入、XSS、敏感信息、鉴权绕过、加密学误用、反序列化、命令执行
* 形成 `security_findings` + `severity` 聚合（Critical/High/Medium/Low）

**降级**：

* 外部扫描器故障 ⇒ 最小化 LLM 规则检查 + 基础黑名单匹配

> 并行阶段的**产出**都会写回 `OverAllState`，键名彼此独立，互不覆盖。

---

# 4. ReportSynthesizerAgent（汇总与生成报告）

**输入**：

* `style_findings`、`logic_findings`、`security_findings`
* `triage_decision`、`review_scope`、`pr_intent`、`diff_stats`、`changed_files`

**聚合步骤**：

1. **合并**：把 3 路结果合并为统一 Schema（建议结构）：

   ```json
   {
     "summary": {...},           // 概览：风险等级、是否阻断、建议下一步
     "metrics": {...},           // 行数、文件数、问题数、严重度分布
     "findings": [
       { "type": "style|logic|security", "file": "...", "line": 123, "rule": "S-001", "severity": "HIGH", "message": "...", "suggestion": "...", "evidence": "..." }
     ],
     "references": [...],        // 知识库/记忆来源（RAG 文档、Memory 线索）
     "triage": {...}             // 初筛结论与理由
   }
   ```
2. **去重/冲突解决**：相同文件+行+相似描述合并，严重度取最大。
3. **打分**：基于数量、严重度、敏感目录权重，给出**总体风险等级**与**是否建议阻断合并**。
4. **格式化**：产出 Markdown / JSON（CI 评论用）/ HTML（控制台查看）。

**输出**：

* `review_report_markdown`
* `review_report_json`
* `review_summary`（给机器人评论的精简版）

**后置动作**（在 `END` 或外围协调器中）：

* 评论到 PR、打标签（如 `needs-fix`）、失败则阻断流水线（可配置）

---

# 5. 失败与降级（容错策略）

* **Triage 拦截**：体量或文件过多 ⇒ 直接返回“建议拆分”，避免后续浪费算力。
* **RAG 降级**：知识库检索失败 ⇒ 走默认规范提示 + 规则集。
* **Memory 降级**：历史记忆不可用 ⇒ 只做当前 patch 的上下文检查。
* **Security 降级**：外部扫描器挂了 ⇒ LLM + 基础规则，标注“有限扫描”。
* **并行阶段**：任何单个 Agent 超时/失败，不影响其他 Agent；在汇总报告中标注“该项不可用”。

---

# 6. 数据契约（关键 State 键约定）

* 输入基础：

    * `review_task`（DTO）、`diff_content`（String）
* 协调员产出：

    * `repo_name`、`pr_number`、`pr_title`、`pr_author`
    * `base_sha`、`head_sha`
    * `changed_files`（String\[]）
    * `diff_stats`（Map：`lines_added/lines_deleted/files_changed/total_changes`）
    * `review_config`（Map：策略开关）
* Triage 产出：

    * `triage_decision`、`triage_message`、`needs_detailed_review`
    * `change_type`、`review_scope[]`、`pr_intent`
* 并行专家产出：

    * `style_findings[]`、`logic_findings[]`、`security_findings[]`
* 汇总产出：

    * `review_report_markdown`、`review_report_json`、`review_summary`

> 坚持**只追加、不覆盖他人键**；若要覆盖，用命名空间：`style.* / logic.* / security.*`。

---

# 7. 性能与可观测性

* **并行度**：`ParallelStarter` 控制线程池/超时；单 Agent 超时不拖累整体。
* **采样**：LLM 输入只取 **diff 片段**（如每文件前 N 行上下文），控制 token。
* **监控**：对每个 Agent 记录：

    * 执行耗时、调用次数、LLM token 用量、知识库命中率、错误率
* **幂等**：以 `pr_number + head_sha` 作为幂等键，避免重复评论和重复审查。

---

# 8. 典型一次运行（举例）

1. Webhook → `review_task=A/B#123`、`diff_content=...`
2. 协调员：产出 `changed_files=12`、`total_changes=480`
3. Triage：`change_type=mixed`、`needs_detailed_review=true`、`review_scope=[style,logic,security]`
4. 并行：

    * Style（RAG）：命中“命名规范/日志规范”，出 8 条建议
    * Logic（Memory）：发现历史约束“X接口需幂等”，当前改动有风险，出 2 条高优
    * Security：发现外部输入未校验，1 条高危
5. 汇总：`review_report_markdown`（顶部红黄绿总体评估 + 分项问题表 + 引用）
6. 终点：把报告评论到 PR，若有 High/Critical 则打 `needs-fix` 标签或阻断。

---



