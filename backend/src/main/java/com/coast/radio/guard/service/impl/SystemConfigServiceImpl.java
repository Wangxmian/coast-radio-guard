package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.dto.config.SystemConfigBatchUpdateDTO;
import com.coast.radio.guard.dto.config.SystemConfigItemDTO;
import com.coast.radio.guard.entity.SystemConfig;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.SystemConfigMapper;
import com.coast.radio.guard.service.SystemConfigService;
import com.coast.radio.guard.vo.config.SystemConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.coast.radio.guard.common.constants.ResultCode.BAD_REQUEST;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String TYPE_MONITOR = "MONITOR";
    private static final List<SystemConfig> DEFAULT_CONFIGS = buildDefaultConfigs();

    private final SystemConfigMapper systemConfigMapper;

    public SystemConfigServiceImpl(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<SystemConfigVO> listConfigs() {
        ensureDefaults();
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId))
            .stream()
            .map(this::toVO)
            .toList();
    }

    @Override
    public Map<String, List<SystemConfigVO>> listGroupedConfigs() {
        ensureDefaults();
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId))
            .stream()
            .map(this::toVO)
            .collect(Collectors.groupingBy(
                vo -> vo.getConfigType() == null ? "DEFAULT" : vo.getConfigType(),
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(SystemConfigBatchUpdateDTO dto) {
        ensureDefaults();

        for (SystemConfigItemDTO item : dto.getConfigs()) {
            SystemConfig row = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, item.getConfigKey())
                .last("LIMIT 1"));
            if (row == null) {
                throw new BusinessException(BAD_REQUEST, "未知配置项: " + item.getConfigKey());
            }
            row.setConfigValue(item.getConfigValue());
            row.setUpdateTime(LocalDateTime.now());
            systemConfigMapper.updateById(row);
        }
    }

    private void ensureDefaults() {
        List<SystemConfig> existing = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
            .select(SystemConfig::getConfigKey));
        Map<String, Boolean> existingMap = existing.stream()
            .map(SystemConfig::getConfigKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(key -> key, key -> true, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();
        for (SystemConfig def : DEFAULT_CONFIGS) {
            if (existingMap.containsKey(def.getConfigKey())) {
                continue;
            }
            SystemConfig row = new SystemConfig();
            row.setConfigKey(def.getConfigKey());
            row.setConfigValue(def.getConfigValue());
            row.setConfigType(def.getConfigType());
            row.setDescription(def.getDescription());
            row.setCreateTime(now);
            row.setUpdateTime(now);
            systemConfigMapper.insert(row);
        }
    }

    private SystemConfigVO toVO(SystemConfig row) {
        return SystemConfigVO.builder()
            .id(row.getId())
            .configKey(row.getConfigKey())
            .configValue(row.getConfigValue())
            .configType(row.getConfigType())
            .description(row.getDescription())
            .createTime(row.getCreateTime())
            .updateTime(row.getUpdateTime())
            .build();
    }

    private static List<SystemConfig> buildDefaultConfigs() {
        List<SystemConfig> list = new ArrayList<>();
        list.add(defaultConfig("hotwordDictionary", "mayday\nfire\ncollision\nman overboard", "值守热词词典"));
        list.add(defaultConfig("riskThreshold", "70", "风险阈值(0-100)"));
        list.add(defaultConfig("autoAlarmEnabled", "true", "是否启用自动报警"));
        list.add(defaultConfig("modelDescription", "VAD + SE + ASR + LLM provider chain", "模型配置说明"));
        list.add(defaultConfig("correctionEnabled", "true", "是否启用转录纠错"));
        list.add(defaultConfig("analysisUseCorrectedTranscript", "true", "分析与告警是否优先使用纠错文本"));
        list.add(defaultConfig("vadEnabled", "true", "VAD能力开关"));
        list.add(defaultConfig("asrEnabled", "true", "ASR能力开关"));
        list.add(defaultConfig("llmEnabled", "true", "LLM能力开关"));
        return list;
    }

    private static SystemConfig defaultConfig(String key, String value, String desc) {
        SystemConfig row = new SystemConfig();
        row.setConfigKey(key);
        row.setConfigValue(value);
        row.setConfigType(TYPE_MONITOR);
        row.setDescription(desc);
        return row;
    }
}
