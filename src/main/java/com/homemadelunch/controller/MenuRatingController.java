package com.homemadelunch.controller;

import com.homemadelunch.common.Result;
import com.homemadelunch.entity.*;
import com.homemadelunch.service.MenuRatingService;
import com.homemadelunch.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 */
@Slf4j
@RestController
@RequestMapping("/rating")
public class MenuRatingController {

    @Autowired
    private MenuRatingService menuRatingService;

    /**
     * 提交/更新评价
     */
    @PostMapping("/submit")
    public Result<MenuRating> submitRating(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        String dateStr = (String) params.get("date");
        LocalDate date = LocalDate.parse(dateStr);
        Integer rating = (Integer) params.get("rating");
        String comment = (String) params.get("comment");
        MenuRating result = menuRatingService.submitRating(user, date, rating, comment);
        return Result.ok("评价提交成功，感谢你的反馈~ 💝", result);
    }

    /**
     * 获取某天的评价
     */
    @GetMapping("/day")
    public Result<List<MenuRating>> getDayRatings(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        List<MenuRating> ratings = menuRatingService.getRatingsByDate(user.getFamilyId(), date);
        return Result.ok(ratings);
    }

    /**
     * 获取我对某天的评价
     */
    @GetMapping("/my")
    public Result<MenuRating> getMyRating(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        MenuRating rating = menuRatingService.getUserRating(user.getFamilyId(), user.getId(), date);
        return Result.ok(rating);
    }
}
