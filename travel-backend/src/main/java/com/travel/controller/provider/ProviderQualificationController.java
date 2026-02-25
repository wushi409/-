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

/**
 * 类说明：ProviderQualificationController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api/provider")
@RequireRole({Constants.ROLE_PROVIDER})
@RequiredArgsConstructor
public class ProviderQualificationController {

    private final ProviderQualificationMapper qualificationMapper;

    /**
     * 方法说明：detail
     * 1. 负责处理 detail 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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
     * 方法说明：submit
     * 1. 负责处理 submit 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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
     * 方法说明：applyPayload
     * 1. 负责处理 applyPayload 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    private void applyPayload(ProviderQualification target, ProviderQualification payload) {
        target.setCompanyName(payload.getCompanyName());
        target.setLicenseNo(payload.getLicenseNo());
        target.setLicenseImage(payload.getLicenseImage());
        target.setContactPerson(payload.getContactPerson());
        target.setContactPhone(payload.getContactPhone());
    }
}
