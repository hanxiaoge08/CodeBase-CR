# repoId 和 taskId 字段修复说明

## 问题描述

在原有的ES索引逻辑中，存在以下问题：

1. **字段混淆**: `repoId` 和 `taskId` 都被设置为相同的值（通常是仓库名称）
2. **概念不清**: `repoId` 应该是Git仓库名称，`taskId` 应该是任务ID
3. **数据缺失**: Catalogue实体在插入ES时缺少 `repoId` 字段
4. **数据库schema**: catalogue表缺少 `repo_id` 字段

## 修复内容

### 1. 概念澄清

| 字段 | 含义 | 示例 | 用途 |
|------|------|------|------|
| `repoId` | Git仓库名称/项目名称 | `CodeBase-Wiki`, `user/project` | 项目级别的数据隔离 |
| `taskId` | 具体的任务ID | `task_123`, `wiki_gen_456` | 任务级别的数据管理 |

### 2. 代码修复

#### ESIntegrationServiceImpl.java
```java
// 修复前：
codeChunk.setRepoId(repositoryId);
codeChunk.setTaskId(repositoryId); // 错误：使用了相同的值

// 修复后：
codeChunk.setRepoId(repositoryId);  // Git仓库名称
codeChunk.setTaskId(taskId);        // 任务ID
```

#### DocumentProcessingService.java
```java
// 修复前：
Catalogue catalogue = new Catalogue();
catalogue.setTaskId(task.getTaskId());
// 缺少 repoId 设置

// 修复后：
Catalogue catalogue = new Catalogue();
catalogue.setTaskId(task.getTaskId());
catalogue.setRepoId(repositoryId);  // 新增repoId设置
```

#### 数据库Schema
```sql
-- 修复前：
CREATE TABLE catalogue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  task_id TEXT,
  -- 缺少 repo_id 字段
  ...
);

-- 修复后：
CREATE TABLE catalogue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  task_id TEXT,
  repo_id TEXT,  -- 新增字段
  ...
);
```

### 3. 数据流程

#### 原始流程（有问题）
```
项目名称(projectName) 
    ↓
repositoryId = projectName
    ↓
repoId = repositoryId
taskId = repositoryId  ← 错误：两个字段相同
```

#### 修复后流程
```
项目名称(projectName) + 任务ID(taskId)
    ↓
repositoryId = projectName || taskId
    ↓
repoId = repositoryId     ← Git仓库名称
taskId = task.getTaskId() ← 实际任务ID
```

### 4. 检索逻辑优化

#### EnhancedHybridSearchService
- 主要基于 `taskId` 进行过滤检索
- `repoId` 作为辅助字段，保持向后兼容

#### CodeReviewESService
- 优先使用 `taskId` 进行检索过滤
- 如果没有 `taskId`，回退使用 `repositoryId`

### 5. 数据迁移

#### 迁移脚本
执行 `migration_add_repo_id.sql` 以：
1. 为现有catalogue表添加 `repo_id` 字段
2. 将现有的 `task_id` 值复制到 `repo_id`（作为默认值）
3. 创建必要的索引

#### 迁移步骤
```bash
# 1. 停止应用
sudo systemctl stop codebase-wiki

# 2. 备份数据库
cp data/codebasewiki_db.sqlite data/codebasewiki_db.sqlite.backup

# 3. 执行迁移
sqlite3 data/codebasewiki_db.sqlite < src/main/resources/migration_add_repo_id.sql

# 4. 启动应用
sudo systemctl start codebase-wiki
```

### 6. 验证方法

#### 检查ES索引数据
```bash
# 检查代码块索引
curl -X GET "localhost:9200/code_chunks_index/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "size": 1,
  "_source": ["repoId", "taskId"]
}'

# 检查文档索引  
curl -X GET "localhost:9200/documents_index/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "size": 1, 
  "_source": ["repoId", "taskId"]
}'
```

#### 检查数据库数据
```sql
-- 检查catalogue表的repo_id字段
SELECT task_id, repo_id, name FROM catalogue LIMIT 5;

-- 验证字段值的差异
SELECT 
    COUNT(*) as total,
    COUNT(CASE WHEN task_id = repo_id THEN 1 END) as same_values,
    COUNT(CASE WHEN task_id != repo_id THEN 1 END) as different_values
FROM catalogue;
```

## 使用建议

### 1. 新项目创建
```java
// 创建任务时
Task task = new Task();
task.setTaskId("wiki_gen_" + System.currentTimeMillis());  // 唯一任务ID
task.setProjectName("MyProject");  // Git仓库名称

// 索引时会正确设置：
// repoId = "MyProject"
// taskId = "wiki_gen_1234567890"
```

### 2. 检索时优先级
```java
// 1. 优先使用taskId过滤
String searchTaskId = request.getTaskId();

// 2. 如果没有taskId，使用repositoryId
if (!StringUtils.hasText(searchTaskId)) {
    searchTaskId = request.getRepositoryId();
}

// 3. 执行检索
List<SearchResultDTO> results = enhancedHybridSearchService.hybridSearch(
    query, searchTaskId, topK);
```

### 3. 监控和日志
- 检索时记录使用的过滤字段（taskId vs repositoryId）
- 定期检查ES索引中repoId和taskId的数据质量
- 监控检索结果的相关性和准确性

## 注意事项

1. **向后兼容**: 修复后的代码保持与现有API的兼容性
2. **数据一致性**: 确保新旧数据在ES索引中字段设置正确
3. **性能影响**: 新增字段和索引可能对写入性能有轻微影响
4. **测试验证**: 在生产环境部署前充分测试检索功能
