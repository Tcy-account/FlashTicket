package com.flashticket.common.dto;

import java.util.List;

/**
 * 通用分页结果（列表接口统一，契约见 doc/M00-工程初始化.md）。
 */
public record PageResult<T>(List<T> items, long total, long page, long pageSize) {
}
