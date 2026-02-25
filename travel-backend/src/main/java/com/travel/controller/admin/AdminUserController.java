package com.travel.controller.admin;

import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.User;
import com.travel.entity.ProviderQualification;
import com.travel.mapper.ProviderQualificationMapper;
import com.travel.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 类说明：AdminUserController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole({Constants.ROLE_ADMIN})
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final ProviderQualificationMapper qualificationMapper;

    @GetMapping("/users")
    public Result<?> listUsers(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer role) {
        return Result.success(userService.listUsers(page, size, keyword, role));
    }

    /**
     * 方法说明：addUser
     * 1. 负责处理 addUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/users")
    public Result<?> addUser(@RequestBody User user) {
        userService.addUser(user);
        return Result.success("添加成功");
    }

    /**
     * 方法说明：updateUser
     * 1. 负责处理 updateUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success("更新成功");
    }

    /**
     * 方法说明：updateStatus
     * 1. 负责处理 updateStatus 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/users/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> params) {
        userService.updateStatus(id, params.get("status"));
        return Result.success("操作成功");
    }

    /**
     * 方法说明：deleteUser
     * 1. 负责处理 deleteUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }

    // ===== 资质审核 =====

    @GetMapping("/qualifications")
    public Result<?> listQualifications(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) Integer auditStatus) {
        Page<ProviderQualification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProviderQualification> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(ProviderQualification::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(ProviderQualification::getCreateTime);
        return Result.success(qualificationMapper.selectPage(pageParam, wrapper));
    }

    /**
     * 方法说明：audit
     * 1. 负责处理 audit 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/qualifications/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestBody java.util.Map<String, Object> params) {
        ProviderQualification q = qualificationMapper.selectById(id);
        q.setAuditStatus((Integer) params.get("auditStatus"));
        q.setAuditRemark((String) params.get("auditRemark"));
        qualificationMapper.updateById(q);
        return Result.success("审核完成");
    }
}
