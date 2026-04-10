package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.channel.ChannelCreateDTO;
import com.coast.radio.guard.dto.channel.ChannelUpdateDTO;
import com.coast.radio.guard.service.ChannelService;
import com.coast.radio.guard.vo.channel.ChannelVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public Result<List<ChannelVO>> listChannels() {
        return Result.ok(channelService.listChannels());
    }

    @GetMapping("/{id}")
    public Result<ChannelVO> getChannel(@PathVariable Long id) {
        return Result.ok(channelService.getChannel(id));
    }

    @PostMapping
    public Result<Map<String, Long>> createChannel(@Valid @RequestBody ChannelCreateDTO dto) {
        Long id = channelService.createChannel(dto);
        return Result.ok("创建成功", Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<Void> updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelUpdateDTO dto) {
        channelService.updateChannel(id, dto);
        return Result.ok("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteChannel(@PathVariable Long id) {
        channelService.deleteChannel(id);
        return Result.ok("删除成功", null);
    }
}
