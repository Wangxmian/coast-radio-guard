package com.coast.radio.guard.vo.alarm;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlarmAuditLogVO {
    private Long id;
    private Long alarmId;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private Long operatorUserId;
    private String operatorUsername;
    private String remark;
    private LocalDateTime createTime;
}
