package com.homemadelunch.controller;

import com.homemadelunch.common.Result;
import com.homemadelunch.entity.User;
import com.homemadelunch.service.UserService;
import com.homemadelunch.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        if (code == null || code.isEmpty()) {
            return Result.fail("登录码不能为空哦~");
        }
        User user = userService.loginOrRegister(code);
        return Result.ok("登录成功", user);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        User user = UserContext.getCurrentUser(request);
        return Result.ok(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Result<User> updateUser(HttpServletRequest request, @RequestBody Map<String, String> params) {
        User user = UserContext.getCurrentUser(request);
        String nickname = params.get("nickname");
        String avatarUrl = params.get("avatarUrl");
        User updated = userService.updateUser(user.getId(), nickname, avatarUrl);
        return Result.ok("更新成功~", updated);
    }
}
