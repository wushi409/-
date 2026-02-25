/**
 * 用户领域核心业务：登录注册、资料维护、后台用户管理。
 */
package com.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Constants;
import com.travel.common.PageResult;
import com.travel.common.exception.BusinessException;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import com.travel.utils.JwtUtils;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    /**
     * 登录主链路（答辩可直接讲这 3 步）。
     *
     * 1. 先按用户名查询用户；
     * 2. 再校验密码和账号状态；
     * 3. 最后生成 token，返回“token + 脱敏后的用户信息”。
     */
    public Map<String, Object> login(String username, String password) {
        // 第一步：查用户是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户名不存在");
        }
        // 第二步：校验密码（数据库里是 MD5）
        if (!DigestUtil.md5Hex(password).equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        // 第三步：校验账号状态，禁用账号禁止登录
        if (user.getStatus() == Constants.STATUS_DISABLED) {
            throw new BusinessException("账号已被禁用");
        }

        // 第四步：签发 token，并返回前端需要的登录结果
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", sanitizeUser(user));
        return result;
    }

    /**
     * 注册主链路。
     * 先检查用户名唯一，再补默认字段并落库。
     */
    public void register(User user) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(DigestUtil.md5Hex(user.getPassword()));
        user.setStatus(Constants.STATUS_ENABLED);
        if (user.getRole() == null) {
            user.setRole(Constants.ROLE_USER);
        }
        userMapper.insert(user);
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public User getInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return sanitizeUser(user);
    }

    public void updateProfile(Long userId, User user) {
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        existing.setNickname(user.getNickname());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        existing.setAvatar(user.getAvatar());
        userMapper.updateById(existing);
    }

    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        // 改密码必须先验证旧密码，避免被越权修改
        if (!DigestUtil.md5Hex(oldPassword).equals(user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(DigestUtil.md5Hex(newPassword));
        userMapper.updateById(user);
    }

    // ===== 管理员功能 =====

    public PageResult<User> listUsers(Integer page, Integer size, String keyword, Integer role) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public void updateStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
    }

    public void addUser(User user) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(DigestUtil.md5Hex(user.getPassword()));
        user.setStatus(Constants.STATUS_ENABLED);
        userMapper.insert(user);
    }

    public void updateUser(User user) {
        User existing = userMapper.selectById(user.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(DigestUtil.md5Hex(user.getPassword()));
        } else {
            user.setPassword(existing.getPassword());
        }
        userMapper.updateById(user);
    }

    /**
     * 返回给前端前做脱敏，避免密码字段外泄。
     */
    private User sanitizeUser(User user) {
        user.setPassword(null);
        return user;
    }
}
