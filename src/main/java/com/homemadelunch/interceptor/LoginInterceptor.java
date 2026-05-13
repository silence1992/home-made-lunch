package com.homemadelunch.interceptor;

import com.homemadelunch.entity.User;
import com.homemadelunch.mapper.UserMapper;
import com.homemadelunch.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器 - 通过请求头中的token(userId)识别用户
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("X-Token");
        if (token == null || token.isEmpty()) {
            throw new BizException(401, "请先登录哦~");
        }

        try {
            Long userId = Long.parseLong(token);
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BizException(401, "用户不存在，请重新登录~");
            }
            // 将用户信息存入request
            request.setAttribute("currentUser", user);
            return true;
        } catch (NumberFormatException e) {
            throw new BizException(401, "登录信息无效，请重新登录~");
        }
    }
}
