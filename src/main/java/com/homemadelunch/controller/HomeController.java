package com.homemadelunch.controller;

import com.homemadelunch.common.Result;
import com.homemadelunch.entity.User;
import com.homemadelunch.service.GreetingService;
import com.homemadelunch.service.OperationLogService;
import com.homemadelunch.entity.OperationLog;
import com.homemadelunch.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 首页控制器
 */
@Slf4j
@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private GreetingService greetingService;
    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取首页问候信息
     */
    @GetMapping("/greeting")
    public Result<Map<String, Object>> getGreeting(HttpServletRequest request) {
        User user = UserContext.getCurrentUser(request);
        Map<String, Object> greeting = greetingService.getGreeting(user);
        return Result.ok(greeting);
    }

    /**
     * 获取最近操作记录
     */
    @GetMapping("/logs")
    public Result<List<OperationLog>> getRecentLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "20") int limit) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        List<OperationLog> logs = operationLogService.getLogsByFamily(user.getFamilyId(), limit);
        return Result.ok(logs);
    }
}
