package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.service.UserRouteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类说明：UserRouteController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/routes")
@RequireRole({Constants.ROLE_USER})
@RequiredArgsConstructor
public class UserRouteController {

    private final UserRouteService userRouteService;

    @GetMapping
    public Result<?> list(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) String keyword) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userRouteService.listUserRoutes(userId, page, size, keyword));
    }

    /**
     * 方法说明：resources
     * 1. 负责处理 resources 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/resources")
    public Result<?> resources(@RequestParam(required = false) Long destinationId) {
        return Result.success(userRouteService.getResources(destinationId));
    }

    /**
     * 方法说明：detail
     * 1. 负责处理 detail 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/{id}")
    public Result<?> detail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userRouteService.getRouteDetail(userId, id));
    }

    /**
     * 方法说明：create
     * 1. 负责处理 create 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody UserRouteService.RouteSaveRequest route) {
        Long userId = (Long) request.getAttribute("userId");
        Long routeId = userRouteService.createRoute(userId, route);
        return Result.success("route created", routeId);
    }

    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request,
                            @PathVariable Long id,
                            @RequestBody UserRouteService.RouteSaveRequest route) {
        Long userId = (Long) request.getAttribute("userId");
        userRouteService.updateRoute(userId, id, route);
        return Result.success("route updated");
    }

    /**
     * 方法说明：delete
     * 1. 负责处理 delete 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        userRouteService.deleteRoute(userId, id);
        return Result.success("route deleted");
    }
}
