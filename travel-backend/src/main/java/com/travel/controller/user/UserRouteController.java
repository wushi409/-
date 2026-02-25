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

    @GetMapping("/resources")
    public Result<?> resources(@RequestParam(required = false) Long destinationId) {
        return Result.success(userRouteService.getResources(destinationId));
    }

    @GetMapping("/{id}")
    public Result<?> detail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userRouteService.getRouteDetail(userId, id));
    }

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

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        userRouteService.deleteRoute(userId, id);
        return Result.success("route deleted");
    }
}
