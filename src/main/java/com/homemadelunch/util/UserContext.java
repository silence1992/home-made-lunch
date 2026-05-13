package com.homemadelunch.util;

import com.homemadelunch.entity.User;
import com.homemadelunch.common.BizException;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户上下文工具
 */
public class UserContext {

    public static User getCurrentUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            throw new BizException(401, "请先登录哦~");
        }
        return user;
    }
}
