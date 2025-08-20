CREATE TABLE IF NOT EXISTS task (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  task_id TEXT NOT NULL,
  project_name TEXT,
  project_url TEXT,
  user_name TEXT,
  status INTEGER,
  fail_reason TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS catalogue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  task_id TEXT,
  repo_id TEXT,
  catalogue_id TEXT,
  parent_catalogue_id TEXT,
  name TEXT,
  title TEXT,
  prompt TEXT,
  dependent_file TEXT,
  children TEXT,
  content TEXT,
  status INTEGER,
  fail_reason TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Spring AI Memory 使用的表会自动创建，不需要手动建表
-- 记忆数据存储在 chat_memory 表中，格式：
-- conversation_id: task_{taskId}_user_{userId}
-- 
-- 如果需要查看记忆数据，可以使用：
-- SELECT conversation_id, COUNT(*) FROM chat_memory 
-- WHERE conversation_id LIKE 'task_%_user_%' 
-- GROUP BY conversation_id;