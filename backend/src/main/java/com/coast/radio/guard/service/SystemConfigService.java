package com.coast.radio.guard.service;

import com.coast.radio.guard.dto.config.SystemConfigBatchUpdateDTO;
import com.coast.radio.guard.vo.config.SystemConfigVO;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    List<SystemConfigVO> listConfigs();

    Map<String, List<SystemConfigVO>> listGroupedConfigs();

    void updateConfigs(SystemConfigBatchUpdateDTO dto);
}
