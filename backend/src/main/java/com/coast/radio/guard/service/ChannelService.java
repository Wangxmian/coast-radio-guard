package com.coast.radio.guard.service;

import com.coast.radio.guard.dto.channel.ChannelCreateDTO;
import com.coast.radio.guard.dto.channel.ChannelUpdateDTO;
import com.coast.radio.guard.vo.channel.ChannelVO;

import java.util.List;

public interface ChannelService {

    List<ChannelVO> listChannels();

    ChannelVO getChannel(Long id);

    Long createChannel(ChannelCreateDTO dto);

    void updateChannel(Long id, ChannelUpdateDTO dto);

    void deleteChannel(Long id);
}
