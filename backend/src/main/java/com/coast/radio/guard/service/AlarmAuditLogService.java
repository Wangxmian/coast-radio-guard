package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.alarm.AlarmAuditLogVO;

import java.util.List;

public interface AlarmAuditLogService {

    void recordAction(Long alarmId,
                      String actionType,
                      String fromStatus,
                      String toStatus,
                      String remark,
                      String operatorUsername);

    List<AlarmAuditLogVO> listByAlarmId(Long alarmId);
}
