package com.homemadelunch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homemadelunch.common.BizException;
import com.homemadelunch.entity.User;
import com.homemadelunch.mapper.UserMapper;
import com.homemadelunch.util.AvatarGenerator;
import com.homemadelunch.util.NicknameGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 */
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 微信登录/注册
     * 开发阶段：直接用code作为openid模拟
     */
    public User loginOrRegister(String code) {
        // 开发阶段直接用code作为openid
        String openid = code;

        // 查找已有用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        if (user != null) {
            return user;
        }

        // 新用户注册
        user = new User();
        user.setOpenid(openid);
        user.setNickname(NicknameGenerator.generate());
        user.setAvatarUrl(AvatarGenerator.generate());
        userMapper.insert(user);

        logService.log(user, "USER", "CREATE", "新用户注册: " + user.getNickname());
        log.info("新用户注册: id={}, nickname={}", user.getId(), user.getNickname());
        return user;
    }

    /**
     * 获取用户信息
     */
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在~");
        }
        return user;
    }

    /**
     * 更新用户信息
     */
    public User updateUser(Long userId, String nickname, String avatarUrl) {
        User user = getUserById(userId);
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }
        userMapper.updateById(user);
        logService.log(user, "USER", "UPDATE", "更新个人信息");
        return user;
    }
}
