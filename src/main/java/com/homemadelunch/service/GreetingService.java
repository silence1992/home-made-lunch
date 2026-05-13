package com.homemadelunch.service;

import com.homemadelunch.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页问候服务
 */
@Slf4j
@Service
public class GreetingService {

    @Autowired
    private DailyMenuService dailyMenuService;
    @Autowired
    private MenuRatingService menuRatingService;
    @Autowired
    private FamilyService familyService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE);

    /**
     * 获取首页问候信息
     */
    public Map<String, Object> getGreeting(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        result.put("today", LocalDate.now().format(DATE_FMT));

        if (user.getFamilyId() == null) {
            result.put("greeting", "你好呀 " + user.getNickname() + "！快去创建或加入一个家庭吧~");
            result.put("hasFamily", false);
            return result;
        }

        result.put("hasFamily", true);
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        boolean isAfter14 = LocalTime.now().isAfter(LocalTime.of(14, 0));

        if ("COOK".equals(user.getRole()) || "ADMIN".equals(user.getRole())) {
            // 厨师视角
            result.putAll(buildCookGreeting(user, today));
        }

        if ("DINER".equals(user.getRole())) {
            // 就餐者视角
            result.putAll(buildDinerGreeting(user, today, tomorrow, isAfter14));
        }

        // ADMIN同时看到两种视角的关键信息
        if ("ADMIN".equals(user.getRole())) {
            // 管理员也能看到明日菜谱提醒
            List<Map<String, Object>> tomorrowMenu = dailyMenuService.getDayMenu(user.getFamilyId(), tomorrow);
            result.put("tomorrowDate", tomorrow.format(DATE_FMT));
            result.put("tomorrowMenu", tomorrowMenu);
        }

        return result;
    }

    /**
     * 构建厨师问候
     */
    private Map<String, Object> buildCookGreeting(User user, LocalDate today) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 今日菜谱
        List<Map<String, Object>> todayMenu = dailyMenuService.getDayMenu(user.getFamilyId(), today);
        result.put("todayMenu", todayMenu);

        // 判断是否为默认菜谱
        boolean isDefaultMenu = todayMenu.size() == 1 &&
            Boolean.TRUE.equals(todayMenu.get(0).get("isDefault"));
        String recipeName = todayMenu.isEmpty() ? "随意" :
            String.valueOf(todayMenu.get(0).get("recipeName"));

        if (isDefaultMenu) {
            if ("随意".equals(recipeName)) {
                result.put("greeting", "今天的菜单由你做主！发挥你的厨艺吧，家人们都期待着呢~ 🍳");
            } else if ("停休".equals(recipeName)) {
                result.put("greeting", "今天好好休息吧！辛苦了，给自己放个假~ ☀️");
            }
        } else {
            String menuNames = todayMenu.stream()
                .map(m -> String.valueOf(m.get("recipeName")))
                .collect(Collectors.joining("、"));
            result.put("greeting", "今天的菜单是：" + menuNames + "，加油做出美味的饭菜吧！💪");
        }

        // 最近评价反馈
        List<MenuRating> recentRatings = menuRatingService.getRecentRatings(user.getFamilyId(), 7);
        if (!recentRatings.isEmpty()) {
            double avgRating = recentRatings.stream()
                .mapToInt(MenuRating::getRating).average().orElse(5.0);
            String feedback;
            if (avgRating >= 4.5) {
                feedback = "最近的餐食评价非常棒！家人们都很满意，继续保持哦~ ⭐⭐⭐⭐⭐";
            } else if (avgRating >= 3.5) {
                feedback = "最近的餐食评价不错，再接再厉，你是最棒的厨师！⭐⭐⭐⭐";
            } else if (avgRating >= 2.5) {
                feedback = "最近的评价一般般，试试换换口味？相信你能做得更好！⭐⭐⭐";
            } else {
                feedback = "最近的评价有点低，别灰心！多和家人沟通喜好，一定会越来越好的~ 💝";
            }
            result.put("ratingFeedback", feedback);
            result.put("avgRating", Math.round(avgRating * 10) / 10.0);
            result.put("recentRatingCount", recentRatings.size());
        }

        return result;
    }

    /**
     * 构建就餐者问候
     */
    private Map<String, Object> buildDinerGreeting(User user, LocalDate today,
                                                     LocalDate tomorrow, boolean isAfter14) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 明日菜谱（始终显示）
        List<Map<String, Object>> tomorrowMenu = dailyMenuService.getDayMenu(user.getFamilyId(), tomorrow);
        result.put("tomorrowDate", tomorrow.format(DATE_FMT));
        result.put("tomorrowMenu", tomorrowMenu);

        boolean tomorrowIsDefault = tomorrowMenu.size() == 1 &&
            Boolean.TRUE.equals(tomorrowMenu.get(0).get("isDefault"));

        if (tomorrowIsDefault) {
            String recipeName = String.valueOf(tomorrowMenu.get(0).get("recipeName"));
            if ("随意".equals(recipeName)) {
                result.put("tomorrowHint", "明天的菜单还没定呢，快去告诉厨师你想吃什么吧~ 🍽️");
            } else if ("停休".equals(recipeName)) {
                result.put("tomorrowHint", "明天厨师休息，记得自己安排午餐哦~ 🏖️");
            }
        } else {
            String menuNames = tomorrowMenu.stream()
                .map(m -> String.valueOf(m.get("recipeName")))
                .collect(Collectors.joining("、"));
            result.put("tomorrowHint", "明天的菜单是：" + menuNames + "，期待吗？😋");
        }

        if (isAfter14) {
            // 14点后提示评价
            List<Map<String, Object>> todayMenu = dailyMenuService.getDayMenu(user.getFamilyId(), today);
            result.put("todayMenu", todayMenu);

            boolean todayIsDefault = todayMenu.size() == 1 &&
                Boolean.TRUE.equals(todayMenu.get(0).get("isDefault"));

            MenuRating myRating = menuRatingService.getUserRating(
                user.getFamilyId(), user.getId(), today);

            if (todayIsDefault) {
                String recipeName = String.valueOf(todayMenu.get(0).get("recipeName"));
                if ("随意".equals(recipeName)) {
                    result.put("greeting", "今天的饭菜是厨师精心准备的惊喜，快来评价一下吧~ 🌟");
                } else if ("停休".equals(recipeName)) {
                    result.put("greeting", "今天厨师休息了，希望你午餐吃得开心~ ☀️");
                }
            } else {
                result.put("greeting", "午餐吃得怎么样？给厨师一个评价鼓励一下吧~ 🌟");
            }

            result.put("canRate", true);
            result.put("hasRated", myRating != null);
            if (myRating != null) {
                result.put("myRating", myRating);
            }
        } else {
            result.put("greeting", "今天也要元气满满哦！看看明天想吃什么~ 🌈");
            result.put("canRate", false);
        }

        return result;
    }
}
