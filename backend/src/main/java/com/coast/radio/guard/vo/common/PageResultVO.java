package com.coast.radio.guard.vo.common;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResultVO<T> {
    private long page;
    private long pageSize;
    private long total;
    private List<T> records;
}
