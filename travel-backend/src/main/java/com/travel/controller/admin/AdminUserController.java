/**
 * 管理员用户与资质审核接口。
 */
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

    @PostMapping("/users")
    public Result<?> addUser(@RequestBody User user) {
        userService.addUser(user);
        return Result.success("添加成功");
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success("更新成功");
    }

    @PutMapping("/users/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> params) {
        userService.updateStatus(id, params.get("status"));
        return Result.success("操作成功");
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }

    // ===== 资质审核 =====

    /**
     * 管理员查看资质审核列表。
     *
     * 用法：
     * 1. 可按 auditStatus 筛选（待审核/已通过/已驳回）；
     * 2. 默认按创建时间倒序，优先处理最新提交。
     */
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
     * 管理员执行审核动作。
     *
     * 前端会传：
     * 1. auditStatus：审核结果（通过/驳回）；
     * 2. auditRemark：审核意见（给服务商看的反馈）。
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
