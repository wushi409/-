/**
 * 游客定制需求接口：提交需求、查看进度、接受/拒绝方案。
 */
package com.travel.controller.user;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.CustomRequest;
import com.travel.service.CustomService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomRequestController {

    private final CustomService customService;

    /**
     * 游客提交定制需求入口。
     * 会把当前登录用户写入请求记录，避免前端伪造 userId。
     */
    @PostMapping("/custom-requests")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> submit(HttpServletRequest request, @RequestBody CustomRequest customRequest) {
        Long userId = (Long) request.getAttribute("userId");
        customRequest.setUserId(userId);
        customService.submitRequest(customRequest);
        return Result.success("request submitted");
    }

    @GetMapping("/custom-requests")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> myRequests(HttpServletRequest request,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(customService.userRequests(userId, page, size));
    }

    @GetMapping("/custom-requests/{id}")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(customService.getRequestByIdForUser(id, userId));
    }

    @GetMapping("/custom-requests/{id}/plan")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> getPlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(customService.getPlanByRequestIdForUser(id, userId));
    }

    /**
     * 游客接受方案入口。
     * 接受后会同步更新“方案状态”和“需求状态”。
     */
    @PutMapping("/custom-plans/{id}/accept")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> acceptPlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        customService.acceptPlan(id, userId);
        return Result.success("plan accepted");
    }

    /**
     * 游客拒绝方案入口。
     * 当前只标记方案为拒绝，后续仍可继续等待/提交新方案。
     */
    @PutMapping("/custom-plans/{id}/reject")
    @RequireRole({Constants.ROLE_USER})
    public Result<?> rejectPlan(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        customService.rejectPlan(id, userId);
        return Result.success("plan rejected");
    }
}
