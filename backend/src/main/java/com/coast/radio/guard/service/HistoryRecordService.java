package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.common.PageResultVO;
import com.coast.radio.guard.vo.history.HistoryRecordVO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public interface HistoryRecordService {

    PageResultVO<HistoryRecordVO> queryHistoryRecords(Long page,
                                                      Long pageSize,
                                                      Long taskId,
                                                      LocalDateTime startTime,
                                                      LocalDateTime endTime,
                                                      String keyword,
                                                      String taskStatus,
                                                      String riskLevel,
                                                      Long channelId,
                                                      String alarmLevel);

    ResponseEntity<byte[]> exportHistoryRecords(LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                Long taskId,
                                                String keyword,
                                                String taskStatus,
                                                String riskLevel,
                                                String alarmLevel);
}
