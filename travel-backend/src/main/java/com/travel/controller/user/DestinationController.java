package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.service.DestinationService;
import com.travel.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：DestinationController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping("/destinations")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword) {
        return Result.success(destinationService.list(page, size, keyword));
    }

    /**
     * 方法说明：detail
     * 1. 负责处理 detail 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/destinations/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(destinationService.getById(id));
    }

    /**
     * 方法说明：hotList
     * 1. 负责处理 hotList 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/destinations/hot")
    public Result<?> hotList(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(destinationService.hotList(limit));
    }
}
