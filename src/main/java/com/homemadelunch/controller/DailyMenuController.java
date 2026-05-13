package com.homemadelunch.controller;

import com.homemadelunch.common.Result;
import com.homemadelunch.entity.*;
import com.homemadelunch.service.DailyMenuService;
import com.homemadelunch.service.OperationLogService;
import com.homemadelunch.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日食谱控制器
 */
@Slf4j
@RestController
@RequestMapping("/menu")
public class DailyMenuController {

    @Autowired
    private DailyMenuService dailyMenuService;
    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取本周食谱
     */
    @GetMapping("/weekly")
    public Result<List<Map<String, Object>>> getWeeklyMenu(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        if (date == null) {
            date = LocalDate.now();
        }
        List<Map<String, Object>> menu = dailyMenuService.getWeeklyMenu(user.getFamilyId(), date);
        return Result.ok(menu);
    }

    /**
     * 获取某天的食谱
     */
    @GetMapping("/day")
    public Result<List<Map<String, Object>>> getDayMenu(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        List<Map<String, Object>> menu = dailyMenuService.getDayMenu(user.getFamilyId(), date);
        return Result.ok(menu);
    }

    /**
     * 设置某天的菜谱（替换）
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/set")
    public Result<?> setDayMenu(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        String dateStr = (String) params.get("date");
        LocalDate date = LocalDate.parse(dateStr);
        List<Long> recipeIds = ((List<Number>) params.get("recipeIds")).stream()
            .map(Number::longValue).collect(Collectors.toList());
        dailyMenuService.setDayMenu(user, date, recipeIds);
        return Result.ok("菜谱设置成功~");
    }

    /**
     * 添加菜谱到某天
     */
    @PostMapping("/add")
    public Result<?> addRecipeToDay(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        String dateStr = (String) params.get("date");
        LocalDate date = LocalDate.parse(dateStr);
        Long recipeId = Long.parseLong(String.valueOf(params.get("recipeId")));
        dailyMenuService.addRecipeToDay(user, date, recipeId);
        return Result.ok("菜谱添加成功~");
    }

    /**
     * 从某天移除菜谱
     */
    @PostMapping("/remove")
    public Result<?> removeRecipeFromDay(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        String dateStr = (String) params.get("date");
        LocalDate date = LocalDate.parse(dateStr);
        Long recipeId = Long.parseLong(String.valueOf(params.get("recipeId")));
        dailyMenuService.removeRecipeFromDay(user, date, recipeId);
        return Result.ok("菜谱已移除~");
    }

    /**
     * 获取某天的操作记录
     */
    @GetMapping("/logs")
    public Result<List<OperationLog>> getMenuLogs(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        List<OperationLog> logs = operationLogService.getLogsByDate(user.getFamilyId(), date);
        return Result.ok(logs);
    }
}
