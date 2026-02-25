package com.travel.controller;

import com.travel.common.Result;
import com.travel.entity.User;
import com.travel.service.UserService;
import com.travel.utils.FileUploadUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 类说明：AuthController
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final FileUploadUtils fileUploadUtils;

    /**
     * 方法说明：login
     * 1. 负责处理 login 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/auth/login")
    public Result<?> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        return Result.success(userService.login(username, password));
    }

    /**
     * 方法说明：register
     * 1. 负责处理 register 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/auth/register")
    public Result<?> register(@RequestBody User user) {
        userService.register(user);
        return Result.success("注册成功");
    }

    /**
     * 方法说明：getInfo
     * 1. 负责处理 getInfo 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @GetMapping("/auth/info")
    public Result<?> getInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getInfo(userId));
    }

    /**
     * 方法说明：updateProfile
     * 1. 负责处理 updateProfile 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/user/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, user);
        return Result.success("更新成功");
    }

    /**
     * 方法说明：updatePassword
     * 1. 负责处理 updatePassword 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PutMapping("/user/password")
    public Result<?> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updatePassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.success("密码修改成功");
    }

    /**
     * 方法说明：upload
     * 1. 负责处理 upload 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileUploadUtils.upload(file);
        return Result.success(url);
    }
}
