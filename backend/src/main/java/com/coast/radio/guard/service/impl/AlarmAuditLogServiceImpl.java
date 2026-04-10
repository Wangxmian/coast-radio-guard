package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.entity.AlarmAuditLog;
import com.coast.radio.guard.entity.SysUser;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.AlarmAuditLogMapper;
import com.coast.radio.guard.mapper.UserMapper;
import com.coast.radio.guard.service.AlarmAuditLogService;
import com.coast.radio.guard.vo.alarm.AlarmAuditLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlarmAuditLogServiceImpl implements AlarmAuditLogService {

    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private final AlarmAuditLogMapper alarmAuditLogMapper;
    private final UserMapper userMapper;

    public AlarmAuditLogServiceImpl(AlarmAuditLogMapper alarmAuditLogMapper, UserMapper userMapper) {
        this.alarmAuditLogMapper = alarmAuditLogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void recordAction(Long alarmId,
                             String actionType,
                             String fromStatus,
                             String toStatus,
                             String remark,
                             String operatorUsername) {
        String username = operatorUsername == null || operatorUsername.isBlank() ? SYSTEM_OPERATOR : operatorUsername;

        Long userId = null;
        if (!SYSTEM_OPERATOR.equals(username)) {
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
            if (user != null) {
                userId = user.getId();
            }
        }

        AlarmAuditLog row = new AlarmAuditLog();
        row.setAlarmId(alarmId);
        row.setActionType(actionType);
        row.setFromStatus(fromStatus);
        row.setToStatus(toStatus);
        row.setOperatorUserId(userId);
        row.setOperatorUsername(username);
        row.setRemark(remark);
        row.setCreateTime(LocalDateTime.now());

        int affected = alarmAuditLogMapper.insert(row);
        if (affected <= 0) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "告警审计日志写入失败");
        }
    }

    @Override
    public List<AlarmAuditLogVO> listByAlarmId(Long alarmId) {
        return alarmAuditLogMapper.selectList(new LambdaQueryWrapper<AlarmAuditLog>()
                .eq(AlarmAuditLog::getAlarmId, alarmId)
                .orderByDesc(AlarmAuditLog::getId))
            .stream()
            .map(this::toVO)
            .toList();
    }

    private AlarmAuditLogVO toVO(AlarmAuditLog row) {
        return AlarmAuditLogVO.builder()
            .id(row.getId())
            .alarmId(row.getAlarmId())
            .actionType(row.getActionType())
            .fromStatus(row.getFromStatus())
            .toStatus(row.getToStatus())
            .operatorUserId(row.getOperatorUserId())
            .operatorUsername(row.getOperatorUsername())
            .remark(row.getRemark())
            .createTime(row.getCreateTime())
            .build();
    }
}
