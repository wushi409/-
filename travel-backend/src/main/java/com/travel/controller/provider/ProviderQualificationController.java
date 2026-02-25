/**
 * 服务商资质接口：提交资质资料并查询审核状态。
 */
package com.travel.controller.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Constants;
import com.travel.common.Result;
import com.travel.config.RequireRole;
import com.travel.entity.ProviderQualification;
import com.travel.mapper.ProviderQualificationMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/provider")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderQualificationController {

    private final ProviderQualificationMapper qualificationMapper;

    /**
     * 服务商查看自己最新一条资质记录。
     * 前端用这个接口回显当前审核状态与审核意见。
     */
    @GetMapping("/qualification")
    public Result<?> detail(HttpServletRequest request) {
        Long providerId = (Long) request.getAttribute("userId");
        ProviderQualification qualification = qualificationMapper.selectOne(
                new LambdaQueryWrapper<ProviderQualification>()
                        .eq(ProviderQualification::getUserId, providerId)
                        .orderByDesc(ProviderQualification::getCreateTime)
                        .last("LIMIT 1"));
        return Result.success(qualification);
    }

    /**
     * 服务商提交/更新资质。
     *
     * 规则：
     * 1. 首次提交：新建记录并置为“待审核”；
     * 2. 重新提交：覆盖原资料并重置为“待审核”；
     * 3. 每次重提都清空审核意见，等待管理员重新判定。
     */
    @PostMapping("/qualification")
    public Result<?> submit(HttpServletRequest request, @RequestBody ProviderQualification payload) {
        Long providerId = (Long) request.getAttribute("userId");

        ProviderQualification qualification = qualificationMapper.selectOne(
                new LambdaQueryWrapper<ProviderQualification>()
                        .eq(ProviderQualification::getUserId, providerId)
                        .orderByDesc(ProviderQualification::getCreateTime)
                        .last("LIMIT 1"));

        if (qualification == null) {
            qualification = new ProviderQualification();
            qualification.setUserId(providerId);
            applyPayload(qualification, payload);
            qualification.setAuditStatus(Constants.AUDIT_PENDING);
            qualification.setAuditRemark(null);
            qualificationMapper.insert(qualification);
        } else {
            applyPayload(qualification, payload);
            qualification.setAuditStatus(Constants.AUDIT_PENDING);
            qualification.setAuditRemark(null);
            qualificationMapper.updateById(qualification);
        }

        return Result.success("qualification submitted", qualification);
    }

    /**
     * 统一把前端提交字段拷贝到实体，避免在多个分支重复赋值。
     */
    private void applyPayload(ProviderQualification target, ProviderQualification payload) {
        target.setCompanyName(payload.getCompanyName());
        target.setLicenseNo(payload.getLicenseNo());
        target.setLicenseImage(payload.getLicenseImage());
        target.setContactPerson(payload.getContactPerson());
        target.setContactPhone(payload.getContactPhone());
    }
}
