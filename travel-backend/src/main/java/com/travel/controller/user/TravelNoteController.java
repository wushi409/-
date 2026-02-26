package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.TravelNote;
import com.travel.service.TravelNoteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 游记接口控制器。
 */
@RestController
@RequestMapping("/api/travel-notes")
@RequiredArgsConstructor
public class TravelNoteController {

    private final TravelNoteService travelNoteService;

    /**
     * 游记列表：支持游客匿名访问。
     */
    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String destination,
                          @RequestParam(defaultValue = "false") Boolean onlyMine) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(travelNoteService.list(page, size, keyword, destination, onlyMine, userId));
    }

    /**
     * 发布游记：仅游客角色可调用。
     */
    @PostMapping
    @RequireRole({Constants.ROLE_USER})
    public Result<?> create(HttpServletRequest request, @RequestBody TravelNote travelNote) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success("游记发布成功", travelNoteService.create(userId, travelNote));
    }
}
