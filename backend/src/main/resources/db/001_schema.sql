CREATE TABLE users (
  id VARCHAR(64) PRIMARY KEY,
  display_name VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL
);

CREATE TABLE templates (
  id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  cover_url VARCHAR(255) NOT NULL,
  description VARCHAR(1024) NOT NULL,
  required_image_count INT NOT NULL,
  accepted_mime_types VARCHAR(255) NOT NULL,
  max_file_size_bytes BIGINT NOT NULL,
  sort_index INT NOT NULL
);

CREATE TABLE assets (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  mime_type VARCHAR(64) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_name VARCHAR(255) NOT NULL,
  created_at DATETIME(3) NOT NULL
);

CREATE TABLE tasks (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  stage VARCHAR(64) NULL,
  failure_stage VARCHAR(64) NULL,
  failure_reason VARCHAR(512) NULL,
  result_url VARCHAR(512) NULL,
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  finished_at DATETIME(3) NULL
);

CREATE INDEX idx_tasks_user_created ON tasks (user_id, created_at);
CREATE INDEX idx_tasks_status_created ON tasks (status, created_at);

CREATE TABLE task_assets (
  task_id VARCHAR(64) NOT NULL,
  asset_id VARCHAR(64) NOT NULL,
  sort_index INT NOT NULL,
  PRIMARY KEY (task_id, sort_index)
);
