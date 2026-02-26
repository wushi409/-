package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.common.exception.BusinessException;
import com.travel.config.RequireRole;
import com.travel.entity.OrderChangeRequest;
import com.travel.service.ChangeRequestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 行程变更申请接口控制器。
 */
@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    /**
     * 用户发起行程变更申请。
     */
    @PostMapping
    @RequireRole({Constants.ROLE_USER})
    public Result<?> create(HttpServletRequest request, @RequestBody OrderChangeRequest payload) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(changeRequestService.create(userId, payload));
    }

    /**
     * 按订单 ID 列表查询变更申请。
     */
    @GetMapping
    @RequireRole({Constants.ROLE_USER, Constants.ROLE_PROVIDER, Constants.ROLE_ADMIN})
    public Result<?> listByOrderIds(HttpServletRequest request, @RequestParam(required = false) String orderIds) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        List<Long> ids = parseOrderIds(orderIds);
        return Result.success(changeRequestService.listByOrderIds(userId, role, ids));
    }

    /**
     * 服务商审核申请。
     */
    @PutMapping("/{id}/review")
    @RequireRole({Constants.ROLE_PROVIDER})
    public Result<?> review(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Long providerId = (Long) request.getAttribute("userId");
        Integer status = null;
        if (payload.get("status") != null) {
            status = Integer.valueOf(String.valueOf(payload.get("status")));
        }
        String reviewRemark = payload.get("reviewRemark") == null ? "" : String.valueOf(payload.get("reviewRemark"));
        return Result.success(changeRequestService.review(id, providerId, status, reviewRemark));
    }

    /**
     * 解析 orderIds 参数（例如：1,2,3）。
     */
    private List<Long> parseOrderIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NumberFormatException ex) {
            throw new BusinessException("orderIds 参数格式错误");
        }
    }
}
