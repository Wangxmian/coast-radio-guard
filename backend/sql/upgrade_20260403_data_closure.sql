USE coast_radio_guard;

SET @col_audio_task_type_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'audio_task' AND column_name = 'task_type'
);
SET @sql_audio_task_type = IF(@col_audio_task_type_exists = 0,
  "ALTER TABLE audio_task ADD COLUMN task_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE' AFTER enhanced_file_path",
  'SELECT 1');
PREPARE stmt_audio_task_type_col FROM @sql_audio_task_type;
EXECUTE stmt_audio_task_type_col;
DEALLOCATE PREPARE stmt_audio_task_type_col;

SET @col_audio_session_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'audio_task' AND column_name = 'source_session_id'
);
SET @sql_audio_session = IF(@col_audio_session_exists = 0,
  "ALTER TABLE audio_task ADD COLUMN source_session_id VARCHAR(64) DEFAULT NULL AFTER task_type",
  'SELECT 1');
PREPARE stmt_audio_session_col FROM @sql_audio_session;
EXECUTE stmt_audio_session_col;
DEALLOCATE PREPARE stmt_audio_session_col;

SET @col_audio_last_transcript_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'audio_task' AND column_name = 'last_transcript_time'
);
SET @sql_audio_last_transcript = IF(@col_audio_last_transcript_exists = 0,
  "ALTER TABLE audio_task ADD COLUMN last_transcript_time DATETIME DEFAULT NULL AFTER execute_time",
  'SELECT 1');
PREPARE stmt_audio_last_transcript_col FROM @sql_audio_last_transcript;
EXECUTE stmt_audio_last_transcript_col;
DEALLOCATE PREPARE stmt_audio_last_transcript_col;

SET @col_asr_language_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'asr_result' AND column_name = 'language'
);
SET @sql_asr_language = IF(@col_asr_language_exists = 0,
  "ALTER TABLE asr_result ADD COLUMN language VARCHAR(32) DEFAULT NULL AFTER confidence",
  'SELECT 1');
PREPARE stmt_asr_language_col FROM @sql_asr_language;
EXECUTE stmt_asr_language_col;
DEALLOCATE PREPARE stmt_asr_language_col;

SET @col_asr_provider_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'asr_result' AND column_name = 'provider'
);
SET @sql_asr_provider = IF(@col_asr_provider_exists = 0,
  "ALTER TABLE asr_result ADD COLUMN provider VARCHAR(64) DEFAULT NULL AFTER language",
  'SELECT 1');
PREPARE stmt_asr_provider_col FROM @sql_asr_provider;
EXECUTE stmt_asr_provider_col;
DEALLOCATE PREPARE stmt_asr_provider_col;

SET @col_asr_source_type_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'asr_result' AND column_name = 'source_type'
);
SET @sql_asr_source_type = IF(@col_asr_source_type_exists = 0,
  "ALTER TABLE asr_result ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE' AFTER provider",
  'SELECT 1');
PREPARE stmt_asr_source_type_col FROM @sql_asr_source_type;
EXECUTE stmt_asr_source_type_col;
DEALLOCATE PREPARE stmt_asr_source_type_col;

SET @col_llm_provider_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'llm_analysis_result' AND column_name = 'provider'
);
SET @sql_llm_provider = IF(@col_llm_provider_exists = 0,
  "ALTER TABLE llm_analysis_result ADD COLUMN provider VARCHAR(64) DEFAULT NULL AFTER reason",
  'SELECT 1');
PREPARE stmt_llm_provider_col FROM @sql_llm_provider;
EXECUTE stmt_llm_provider_col;
DEALLOCATE PREPARE stmt_llm_provider_col;

SET @col_llm_source_type_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'llm_analysis_result' AND column_name = 'source_type'
);
SET @sql_llm_source_type = IF(@col_llm_source_type_exists = 0,
  "ALTER TABLE llm_analysis_result ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'OFFLINE' AFTER provider",
  'SELECT 1');
PREPARE stmt_llm_source_type_col FROM @sql_llm_source_type;
EXECUTE stmt_llm_source_type_col;
DEALLOCATE PREPARE stmt_llm_source_type_col;

SET @col_risk_analysis_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'risk_event' AND column_name = 'analysis_id'
);
SET @sql_risk_analysis_col = IF(@col_risk_analysis_exists = 0,
  "ALTER TABLE risk_event ADD COLUMN analysis_id BIGINT DEFAULT NULL AFTER task_id",
  'SELECT 1');
PREPARE stmt_risk_analysis_col FROM @sql_risk_analysis_col;
EXECUTE stmt_risk_analysis_col;
DEALLOCATE PREPARE stmt_risk_analysis_col;

SET @col_alarm_analysis_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'alarm_record' AND column_name = 'analysis_id'
);
SET @sql_alarm_analysis_col = IF(@col_alarm_analysis_exists = 0,
  "ALTER TABLE alarm_record ADD COLUMN analysis_id BIGINT DEFAULT NULL AFTER task_id",
  'SELECT 1');
PREPARE stmt_alarm_analysis_col FROM @sql_alarm_analysis_col;
EXECUTE stmt_alarm_analysis_col;
DEALLOCATE PREPARE stmt_alarm_analysis_col;

SET @col_alarm_risk_event_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'alarm_record' AND column_name = 'risk_event_id'
);
SET @sql_alarm_risk_event_col = IF(@col_alarm_risk_event_exists = 0,
  "ALTER TABLE alarm_record ADD COLUMN risk_event_id BIGINT DEFAULT NULL AFTER analysis_id",
  'SELECT 1');
PREPARE stmt_alarm_risk_event_col FROM @sql_alarm_risk_event_col;
EXECUTE stmt_alarm_risk_event_col;
DEALLOCATE PREPARE stmt_alarm_risk_event_col;

SET @col_alarm_auto_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'alarm_record' AND column_name = 'is_auto_created'
);
SET @sql_alarm_auto_col = IF(@col_alarm_auto_exists = 0,
  "ALTER TABLE alarm_record ADD COLUMN is_auto_created TINYINT NOT NULL DEFAULT 0 AFTER alarm_status",
  'SELECT 1');
PREPARE stmt_alarm_auto_col FROM @sql_alarm_auto_col;
EXECUTE stmt_alarm_auto_col;
DEALLOCATE PREPARE stmt_alarm_auto_col;

SET @idx_audio_task_type_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audio_task' AND index_name = 'idx_audio_task_type'
);
SET @sql_audio_task_type = IF(@idx_audio_task_type_exists = 0,
  'CREATE INDEX idx_audio_task_type ON audio_task(task_type)', 'SELECT 1');
PREPARE stmt_audio_task_type FROM @sql_audio_task_type;
EXECUTE stmt_audio_task_type;
DEALLOCATE PREPARE stmt_audio_task_type;

SET @idx_audio_task_session_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'audio_task' AND index_name = 'idx_audio_task_session'
);
SET @sql_audio_task_session = IF(@idx_audio_task_session_exists = 0,
  'CREATE INDEX idx_audio_task_session ON audio_task(source_session_id)', 'SELECT 1');
PREPARE stmt_audio_task_session FROM @sql_audio_task_session;
EXECUTE stmt_audio_task_session;
DEALLOCATE PREPARE stmt_audio_task_session;

SET @idx_risk_analysis_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'risk_event' AND index_name = 'idx_risk_analysis_id'
);
SET @sql_risk_analysis = IF(@idx_risk_analysis_exists = 0,
  'CREATE INDEX idx_risk_analysis_id ON risk_event(analysis_id)', 'SELECT 1');
PREPARE stmt_risk_analysis FROM @sql_risk_analysis;
EXECUTE stmt_risk_analysis;
DEALLOCATE PREPARE stmt_risk_analysis;

SET @idx_alarm_analysis_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'alarm_record' AND index_name = 'idx_alarm_analysis_id'
);
SET @sql_alarm_analysis = IF(@idx_alarm_analysis_exists = 0,
  'CREATE INDEX idx_alarm_analysis_id ON alarm_record(analysis_id)', 'SELECT 1');
PREPARE stmt_alarm_analysis FROM @sql_alarm_analysis;
EXECUTE stmt_alarm_analysis;
DEALLOCATE PREPARE stmt_alarm_analysis;

SET @idx_alarm_risk_event_exists = (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'alarm_record' AND index_name = 'idx_alarm_risk_event_id'
);
SET @sql_alarm_risk_event = IF(@idx_alarm_risk_event_exists = 0,
  'CREATE INDEX idx_alarm_risk_event_id ON alarm_record(risk_event_id)', 'SELECT 1');
PREPARE stmt_alarm_risk_event FROM @sql_alarm_risk_event;
EXECUTE stmt_alarm_risk_event;
DEALLOCATE PREPARE stmt_alarm_risk_event;

UPDATE audio_task
SET task_type = 'OFFLINE'
WHERE task_type IS NULL OR task_type = '';

UPDATE asr_result
SET source_type = 'OFFLINE'
WHERE source_type IS NULL OR source_type = '';

UPDATE llm_analysis_result
SET source_type = 'OFFLINE'
WHERE source_type IS NULL OR source_type = '';
