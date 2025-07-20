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
