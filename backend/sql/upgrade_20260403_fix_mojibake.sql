USE coast_radio_guard;

UPDATE radio_channel
SET channel_name = CONVERT(CAST(CONVERT(channel_name USING latin1) AS BINARY) USING utf8mb4)
WHERE channel_name REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE radio_channel
SET remark = CONVERT(CAST(CONVERT(remark USING latin1) AS BINARY) USING utf8mb4)
WHERE remark IS NOT NULL
  AND remark REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE system_config
SET config_value = CONVERT(CAST(CONVERT(config_value USING latin1) AS BINARY) USING utf8mb4)
WHERE config_value IS NOT NULL
  AND config_value REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE system_config
SET description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4)
WHERE description IS NOT NULL
  AND description REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE audio_task
SET original_file_path = CONVERT(CAST(CONVERT(original_file_path USING latin1) AS BINARY) USING utf8mb4)
WHERE original_file_path IS NOT NULL
  AND original_file_path REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE audio_task
SET enhanced_file_path = CONVERT(CAST(CONVERT(enhanced_file_path USING latin1) AS BINARY) USING utf8mb4)
WHERE enhanced_file_path IS NOT NULL
  AND enhanced_file_path REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE audio_task
SET transcript_text = CONVERT(CAST(CONVERT(transcript_text USING latin1) AS BINARY) USING utf8mb4)
WHERE transcript_text IS NOT NULL
  AND transcript_text REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE asr_result
SET transcript_text = CONVERT(CAST(CONVERT(transcript_text USING latin1) AS BINARY) USING utf8mb4)
WHERE transcript_text IS NOT NULL
  AND transcript_text REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE llm_analysis_result
SET event_summary = CONVERT(CAST(CONVERT(event_summary USING latin1) AS BINARY) USING utf8mb4)
WHERE event_summary IS NOT NULL
  AND event_summary REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE llm_analysis_result
SET reason = CONVERT(CAST(CONVERT(reason USING latin1) AS BINARY) USING utf8mb4)
WHERE reason IS NOT NULL
  AND reason REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE entity_result
SET entity_value = CONVERT(CAST(CONVERT(entity_value USING latin1) AS BINARY) USING utf8mb4)
WHERE entity_value IS NOT NULL
  AND entity_value REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE risk_event
SET summary = CONVERT(CAST(CONVERT(summary USING latin1) AS BINARY) USING utf8mb4)
WHERE summary IS NOT NULL
  AND summary REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE alarm_record
SET trigger_reason = CONVERT(CAST(CONVERT(trigger_reason USING latin1) AS BINARY) USING utf8mb4)
WHERE trigger_reason IS NOT NULL
  AND trigger_reason REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';

UPDATE alarm_record
SET handle_remark = CONVERT(CAST(CONVERT(handle_remark USING latin1) AS BINARY) USING utf8mb4)
WHERE handle_remark IS NOT NULL
  AND handle_remark REGEXP 'æ|å|ç|é|è|ê|â|ã|ð|ï|�';
