package com.coast.radio.guard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coast.radio.guard.entity.RadioChannel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChannelMapper extends BaseMapper<RadioChannel> {
}
