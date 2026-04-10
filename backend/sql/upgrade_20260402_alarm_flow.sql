USE coast_radio_guard;

CREATE TABLE IF NOT EXISTS alarm_audit_log (
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

UPDATE alarm_record
SET alarm_status = 'UNHANDLED'
WHERE alarm_status IS NULL OR alarm_status = '' OR alarm_status = 'HANDLED';
