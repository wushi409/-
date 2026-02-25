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

/**
 * 类说明：UserService
 * 1. 负责该业务模块的核心流程编排；
 * 2. 通过分层设计保证职责清晰、便于维护；
 * 3. 为上层调用提供稳定、可复用的能力。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    /**
     * 方法说明：login
     * 1. 负责处理 login 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户名不存在");
        }
        if (!DigestUtil.md5Hex(password).equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        if (user.getStatus() == Constants.STATUS_DISABLED) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", sanitizeUser(user));
        return result;
    }

    /**
     * 方法说明：register
     * 1. 负责处理 register 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
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

    /**
     * 方法说明：getById
     * 1. 负责处理 getById 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 方法说明：getInfo
     * 1. 负责处理 getInfo 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public User getInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return sanitizeUser(user);
    }

    /**
     * 方法说明：updateProfile
     * 1. 负责处理 updateProfile 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
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

    /**
     * 方法说明：updatePassword
     * 1. 负责处理 updatePassword 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (!DigestUtil.md5Hex(oldPassword).equals(user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(DigestUtil.md5Hex(newPassword));
        userMapper.updateById(user);
    }

    // ===== 管理员功能 =====

    /**
     * 方法说明：listUsers
     * 1. 负责处理 listUsers 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
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

    /**
     * 方法说明：updateStatus
     * 1. 负责处理 updateStatus 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void updateStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 方法说明：deleteUser
     * 1. 负责处理 deleteUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
    }

    /**
     * 方法说明：addUser
     * 1. 负责处理 addUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
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

    /**
     * 方法说明：updateUser
     * 1. 负责处理 updateUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
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
     * 方法说明：sanitizeUser
     * 1. 负责处理 sanitizeUser 对应的业务逻辑；
     * 2. 完成参数校验、数据读写与状态变更；
     * 3. 输出处理结果供控制层或调用方继续使用。
     */
    private User sanitizeUser(User user) {
        user.setPassword(null);
        return user;
    }
}
