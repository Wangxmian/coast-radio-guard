package com.coast.radio.guard.service;

import com.coast.radio.guard.dto.alarm.ManualAlarmCreateDTO;
import com.coast.radio.guard.vo.audio.AlarmRecordVO;
import com.coast.radio.guard.vo.alarm.AlarmDetailVO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface AlarmService {

    Long createManualAlarm(ManualAlarmCreateDTO dto);

    List<AlarmRecordVO> listAlarms(String alarmLevel,
                                   String alarmStatus,
                                   String triggerSource,
                                   Long taskId,
                                   LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   String keyword,
                                   Long page,
                                   Long pageSize);

    AlarmDetailVO getAlarmDetail(Long id);

    void acknowledge(Long id, String remark);

    void process(Long id, String remark);

    void resolve(Long id, String remark);

    void close(Long id, String remark);

    void falseAlarm(Long id, String remark);

    void recordAutoCreateAudit(Long alarmId, String remark);

    ResponseEntity<byte[]> exportAlarms(String alarmLevel,
                                        String alarmStatus,
                                        String triggerSource,
                                        Long taskId,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime,
                                        String keyword,
                                        Long page,
                                        Long pageSize);
}
