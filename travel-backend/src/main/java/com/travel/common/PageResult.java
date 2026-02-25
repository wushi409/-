/**
 * PageResult 公共类。
 * 放统一返回结构、常量或通用异常定义。
 */
package com.travel.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long current;
    private long size;

    public PageResult(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }
}
