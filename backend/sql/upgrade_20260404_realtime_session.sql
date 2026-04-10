SET @ddl = (
  SELECT IF(
    EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'realtime_transcript_record'
    ),
    'SELECT 1',
    'CREATE TABLE realtime_transcript_record (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      session_id VARCHAR(64) NOT NULL,
      task_id BIGINT NOT NULL,
      channel_id BIGINT NOT NULL,
      raw_transcript TEXT DEFAULT NULL,
      corrected_transcript TEXT DEFAULT NULL,
      start_time_ms INT DEFAULT NULL,
      end_time_ms INT DEFAULT NULL,
      is_final TINYINT(1) NOT NULL DEFAULT 1,
      has_speech TINYINT(1) NOT NULL DEFAULT 1,
      analysis_id BIGINT DEFAULT NULL,
      alarm_id BIGINT DEFAULT NULL,
      risk_level VARCHAR(32) DEFAULT NULL,
      event_type VARCHAR(64) DEFAULT NULL,
      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_rt_record_session (session_id),
      INDEX idx_rt_record_task (task_id),
      INDEX idx_rt_record_channel (channel_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO system_config (config_key, config_value, config_type, description)
SELECT 'realtimeModeDescription', '准实时 chunk 模式：fsmn-vad 先筛语音，SenseVoiceSmall 识别短音频片段', 'MONITOR', '实时转录实现说明'
WHERE NOT EXISTS (
  SELECT 1 FROM system_config WHERE config_key = 'realtimeModeDescription'
);
