USE coast_radio_guard;

ALTER TABLE alarm_record
  MODIFY COLUMN task_id BIGINT NULL;

ALTER TABLE alarm_record
  ADD COLUMN IF NOT EXISTS channel_id BIGINT NULL AFTER task_id;

SET @idx_alarm_channel_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'alarm_record'
    AND index_name = 'idx_alarm_channel_id'
);
SET @create_alarm_channel_idx_sql = IF(
  @idx_alarm_channel_exists = 0,
  'CREATE INDEX idx_alarm_channel_id ON alarm_record(channel_id)',
  'SELECT 1'
);
PREPARE stmt_alarm_channel_idx FROM @create_alarm_channel_idx_sql;
EXECUTE stmt_alarm_channel_idx;
DEALLOCATE PREPARE stmt_alarm_channel_idx;

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(64) NOT NULL UNIQUE,
  config_value TEXT DEFAULT NULL,
  config_type VARCHAR(64) DEFAULT NULL,
  description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO system_config (config_key, config_value, config_type, description)
VALUES
('hotwordDictionary', 'mayday\nfire\ncollision\nman overboard', 'MONITOR', '值守热词词典'),
('riskThreshold', '70', 'MONITOR', '风险阈值(0-100)'),
('autoAlarmEnabled', 'true', 'MONITOR', '是否启用自动报警'),
('modelDescription', 'VAD + SE + ASR + LLM provider chain', 'MONITOR', '模型配置说明'),
('vadEnabled', 'true', 'MONITOR', 'VAD能力开关'),
('asrEnabled', 'true', 'MONITOR', 'ASR能力开关'),
('llmEnabled', 'true', 'MONITOR', 'LLM能力开关')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  config_type = VALUES(config_type),
  description = VALUES(description),
  update_time = CURRENT_TIMESTAMP;
