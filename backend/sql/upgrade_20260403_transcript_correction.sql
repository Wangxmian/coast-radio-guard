USE coast_radio_guard;

SET @add_raw := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asr_result' AND COLUMN_NAME = 'raw_transcript'
  ),
  'SELECT 1',
  'ALTER TABLE asr_result ADD COLUMN raw_transcript TEXT NULL AFTER transcript_text'
);
PREPARE stmt FROM @add_raw;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_corrected := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asr_result' AND COLUMN_NAME = 'corrected_transcript'
  ),
  'SELECT 1',
  'ALTER TABLE asr_result ADD COLUMN corrected_transcript TEXT NULL AFTER raw_transcript'
);
PREPARE stmt FROM @add_corrected;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_diff := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asr_result' AND COLUMN_NAME = 'correction_diff'
  ),
  'SELECT 1',
  'ALTER TABLE asr_result ADD COLUMN correction_diff TEXT NULL AFTER corrected_transcript'
);
PREPARE stmt FROM @add_diff;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_provider := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asr_result' AND COLUMN_NAME = 'correction_provider'
  ),
  'SELECT 1',
  'ALTER TABLE asr_result ADD COLUMN correction_provider VARCHAR(64) NULL AFTER correction_diff'
);
PREPARE stmt FROM @add_provider;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_fallback := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'asr_result' AND COLUMN_NAME = 'correction_fallback'
  ),
  'SELECT 1',
  'ALTER TABLE asr_result ADD COLUMN correction_fallback TINYINT(1) NOT NULL DEFAULT 0 AFTER correction_provider'
);
PREPARE stmt FROM @add_fallback;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE asr_result
SET raw_transcript = COALESCE(raw_transcript, transcript_text),
    corrected_transcript = COALESCE(corrected_transcript, transcript_text),
    correction_fallback = IF(corrected_transcript IS NULL, 1, correction_fallback)
WHERE raw_transcript IS NULL
   OR corrected_transcript IS NULL;
