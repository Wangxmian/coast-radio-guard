package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.dto.channel.ChannelCreateDTO;
import com.coast.radio.guard.dto.channel.ChannelUpdateDTO;
import com.coast.radio.guard.entity.RadioChannel;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.ChannelMapper;
import com.coast.radio.guard.service.ChannelService;
import com.coast.radio.guard.vo.channel.ChannelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ChannelServiceImpl implements ChannelService {

    private final ChannelMapper channelMapper;

    public ChannelServiceImpl(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    @Override
    public List<ChannelVO> listChannels() {
        return channelMapper.selectList(new LambdaQueryWrapper<RadioChannel>()
                .orderByDesc(RadioChannel::getPriority)
                .orderByDesc(RadioChannel::getId))
            .stream().map(this::toVO).toList();
    }

    @Override
    public ChannelVO getChannel(Long id) {
        RadioChannel channel = requireById(id);
        return toVO(channel);
    }

    @Override
    public Long createChannel(ChannelCreateDTO dto) {
        long exists = channelMapper.selectCount(new LambdaQueryWrapper<RadioChannel>()
            .eq(RadioChannel::getChannelCode, dto.getChannelCode()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "频道编码已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        RadioChannel channel = new RadioChannel();
        channel.setChannelCode(dto.getChannelCode());
        channel.setChannelName(dto.getChannelName());
        channel.setFrequency(dto.getFrequency());
        channel.setPriority(dto.getPriority());
        channel.setStatus(dto.getStatus());
        channel.setRemark(dto.getRemark());
        channel.setCreateTime(now);
        channel.setUpdateTime(now);
        channelMapper.insert(channel);
        log.info("Created channel {}, code={}", channel.getId(), channel.getChannelCode());
        return channel.getId();
    }

    @Override
    public void updateChannel(Long id, ChannelUpdateDTO dto) {
        RadioChannel channel = requireById(id);
        channel.setChannelName(dto.getChannelName());
        channel.setFrequency(dto.getFrequency());
        channel.setPriority(dto.getPriority());
        channel.setStatus(dto.getStatus());
        channel.setRemark(dto.getRemark());
        channel.setUpdateTime(LocalDateTime.now());
        channelMapper.updateById(channel);
    }

    @Override
    public void deleteChannel(Long id) {
        requireById(id);
        channelMapper.deleteById(id);
        log.info("Deleted channel {}", id);
    }

    private RadioChannel requireById(Long id) {
        RadioChannel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "频道不存在");
        }
        return channel;
    }

    private ChannelVO toVO(RadioChannel channel) {
        return ChannelVO.builder()
            .id(channel.getId())
            .channelCode(channel.getChannelCode())
            .channelName(channel.getChannelName())
            .frequency(channel.getFrequency())
            .priority(channel.getPriority())
            .status(channel.getStatus())
            .remark(channel.getRemark())
            .createTime(channel.getCreateTime())
            .updateTime(channel.getUpdateTime())
            .build();
    }
}
