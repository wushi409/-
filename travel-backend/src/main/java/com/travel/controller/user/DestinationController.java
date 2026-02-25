package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.service.DestinationService;
import com.travel.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/destinations/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(destinationService.getById(id));
    }

    @GetMapping("/destinations/hot")
    public Result<?> hotList(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(destinationService.hotList(limit));
    }
}
