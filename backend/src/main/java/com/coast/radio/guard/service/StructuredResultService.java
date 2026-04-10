package com.coast.radio.guard.service;

import com.coast.radio.guard.vo.audio.AnalysisDetailVO;
import com.coast.radio.guard.vo.common.PageResultVO;
import com.coast.radio.guard.vo.structured.StructuredResultListVO;

import java.time.LocalDateTime;
import java.util.Map;

public interface StructuredResultService {

    PageResultVO<StructuredResultListVO> queryStructuredResults(Long page,
                                                                 Long pageSize,
                                                                 Long taskId,
                                                                 String riskLevel,
                                                                 String eventType,
                                                                 String keyword,
                                                                 LocalDateTime startTime,
                                                                 LocalDateTime endTime);

    AnalysisDetailVO getStructuredResultDetail(Long taskId);

    Map<String, Object> getStructuredResultJson(Long taskId);
}
