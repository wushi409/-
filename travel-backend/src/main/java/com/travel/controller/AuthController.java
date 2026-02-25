/**
 * 认证与用户资料接口：登录、注册、个人信息、改密、文件上传。
 */
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final FileUploadUtils fileUploadUtils;

    /**
     * 登录接口入口。
     * 这里只做参数接收与转发，核心校验逻辑在 UserService#login。
     */
    @PostMapping("/auth/login")
    public Result<?> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        return Result.success(userService.login(username, password));
    }

    /**
     * 注册接口入口。
     * service 里会做“重名校验 + 密码加密 + 默认角色设置”。
     */
    @PostMapping("/auth/register")
    public Result<?> register(@RequestBody User user) {
        userService.register(user);
        return Result.success("注册成功");
    }

    @GetMapping("/auth/info")
    public Result<?> getInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getInfo(userId));
    }

    @PutMapping("/user/profile")
    public Result<?> updateProfile(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, user);
        return Result.success("更新成功");
    }

    @PutMapping("/user/password")
    public Result<?> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updatePassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.success("密码修改成功");
    }

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileUploadUtils.upload(file);
        return Result.success(url);
    }
}
