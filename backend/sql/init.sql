CREATE DATABASE IF NOT EXISTS coast_radio_guard DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE coast_radio_guard;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS alarm_record;
DROP TABLE IF EXISTS alarm_audit_log;
DROP TABLE IF EXISTS entity_result;
DROP TABLE IF EXISTS llm_analysis_result;
DROP TABLE IF EXISTS asr_result;
DROP TABLE IF EXISTS risk_event;
DROP TABLE IF EXISTS audio_task;
DROP TABLE IF EXISTS radio_channel;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS system_config;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE radio_channel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_code VARCHAR(64) NOT NULL UNIQUE,
  channel_name VARCHAR(128) NOT NULL,
  frequency VARCHAR(64) NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audio_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  channel_id BIGINT NOT NULL,
  original_file_path VARCHAR(255) NOT NULL,
  enhanced_file_path VARCHAR(255) DEFAULT NULL,
  task_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
  source_session_id VARCHAR(64) DEFAULT NULL,
  task_status VARCHAR(32) NOT NULL DEFAULT 'WAITING',
  se_status VARCHAR(32) NOT NULL DEFAULT 'WAITING',
  asr_status VARCHAR(32) NOT NULL DEFAULT 'WAITING',
  llm_status VARCHAR(32) NOT NULL DEFAULT 'WAITING',
  transcript_text TEXT DEFAULT NULL,
  risk_level VARCHAR(32) DEFAULT NULL,
  duration DECIMAL(10,2) DEFAULT NULL,
  error_msg VARCHAR(255) DEFAULT NULL,
  last_error_msg VARCHAR(255) DEFAULT NULL,
  execute_time DATETIME DEFAULT NULL,
  last_transcript_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time DATETIME DEFAULT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_audio_task_type (task_type),
  INDEX idx_audio_task_session (source_session_id),
  CONSTRAINT fk_audio_task_channel FOREIGN KEY (channel_id) REFERENCES radio_channel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE asr_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  transcript_text TEXT NOT NULL,
  raw_transcript TEXT DEFAULT NULL,
  corrected_transcript TEXT DEFAULT NULL,
  correction_diff TEXT DEFAULT NULL,
  correction_provider VARCHAR(64) DEFAULT NULL,
  correction_fallback TINYINT(1) NOT NULL DEFAULT 0,
  confidence DECIMAL(6,4) DEFAULT NULL,
  language VARCHAR(32) DEFAULT NULL,
  provider VARCHAR(64) DEFAULT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_asr_task_id (task_id),
  CONSTRAINT fk_asr_task FOREIGN KEY (task_id) REFERENCES audio_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE llm_analysis_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  event_type VARCHAR(64) DEFAULT NULL,
  event_summary VARCHAR(500) DEFAULT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  provider VARCHAR(64) DEFAULT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
  raw_response TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_llm_task_id (task_id),
  CONSTRAINT fk_llm_task FOREIGN KEY (task_id) REFERENCES audio_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE entity_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_value VARCHAR(255) NOT NULL,
  confidence DECIMAL(6,4) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_entity_task_id (task_id),
  CONSTRAINT fk_entity_task FOREIGN KEY (task_id) REFERENCES audio_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE risk_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  analysis_id BIGINT DEFAULT NULL,
  risk_level VARCHAR(32) NOT NULL,
  event_type VARCHAR(64) DEFAULT NULL,
  summary VARCHAR(500) DEFAULT NULL,
  source VARCHAR(64) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_risk_task_id (task_id),
  INDEX idx_risk_analysis_id (analysis_id),
  CONSTRAINT fk_risk_task FOREIGN KEY (task_id) REFERENCES audio_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alarm_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT DEFAULT NULL,
  analysis_id BIGINT DEFAULT NULL,
  risk_event_id BIGINT DEFAULT NULL,
  channel_id BIGINT DEFAULT NULL,
  alarm_level VARCHAR(32) NOT NULL,
  trigger_source VARCHAR(64) NOT NULL,
  trigger_reason VARCHAR(500) DEFAULT NULL,
  alarm_status VARCHAR(32) NOT NULL DEFAULT 'UNHANDLED',
  is_auto_created TINYINT NOT NULL DEFAULT 0,
  handle_user VARCHAR(64) DEFAULT NULL,
  handle_time DATETIME DEFAULT NULL,
  handle_remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_alarm_task_id (task_id),
  INDEX idx_alarm_analysis_id (analysis_id),
  INDEX idx_alarm_risk_event_id (risk_event_id),
  INDEX idx_alarm_channel_id (channel_id),
  CONSTRAINT fk_alarm_task FOREIGN KEY (task_id) REFERENCES audio_task(id),
  CONSTRAINT fk_alarm_channel FOREIGN KEY (channel_id) REFERENCES radio_channel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alarm_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  alarm_id BIGINT NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  from_status VARCHAR(32) DEFAULT NULL,
  to_status VARCHAR(32) DEFAULT NULL,
  operator_user_id BIGINT DEFAULT NULL,
  operator_username VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_alarm_audit_alarm_id (alarm_id),
  CONSTRAINT fk_alarm_audit_alarm FOREIGN KEY (alarm_id) REFERENCES alarm_record(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value TEXT DEFAULT NULL,
  config_type VARCHAR(64) DEFAULT NULL,
  description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_user (username, password, real_name, role, status)
VALUES ('admin', '$2y$10$DDnpC5fWjzMY5CuXdo.BueXGzSMmxyRM8bcH6hp2z/vy0DFBkHjIy', '系统管理员', 'ADMIN', 1);

INSERT INTO radio_channel (channel_code, channel_name, frequency, priority, status, remark)
VALUES
('CH-001', '海岸指挥主频道', '156.800MHz', 10, 1, 'VHF应急主频道'),
('CH-002', '港区调度频道', '157.100MHz', 8, 1, '港区日常调度');

INSERT INTO audio_task (channel_id, original_file_path, enhanced_file_path, task_status, se_status, asr_status, llm_status, duration)
VALUES
(1, '/Users/wangxinmian/Downloads/浏览器/demo_result_e006de422e8b11f19392b28606cf9cb5.wav', NULL, 'WAITING', 'WAITING', 'WAITING', 'WAITING', 32.50),
(2, '/Users/wangxinmian/Downloads/浏览器/demo_result_983720102e8911f19392b28606cf9cb5.wav', NULL, 'WAITING', 'WAITING', 'WAITING', 'WAITING', 18.20);

INSERT INTO system_config (config_key, config_value, config_type, description)
VALUES
('hotwordDictionary', '求救\救命\遇险\遇难\紧急求助\请求支援\呼救\SOS\Mayday\起火\火灾\燃烧\碰撞\相撞\触礁\搁浅\落水\人员落水\有人落水\man overboard', 'MONITOR', '值守热词词典'),
('riskThreshold', '70', 'MONITOR', '风险阈值(0-100)'),
('autoAlarmEnabled', 'true', 'MONITOR', '是否启用自动报警'),
('modelDescription', 'VAD + SE + ASR + LLM provider chain', 'MONITOR', '模型配置说明'),
('vadEnabled', 'true', 'MONITOR', 'VAD能力开关'),
('asrEnabled', 'true', 'MONITOR', 'ASR能力开关'),
('llmEnabled', 'true', 'MONITOR', 'LLM能力开关');
