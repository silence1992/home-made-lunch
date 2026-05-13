package com.homemadelunch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homemadelunch.common.BizException;
import com.homemadelunch.entity.*;
import com.homemadelunch.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 食谱评价服务
 */
@Slf4j
@Service
public class MenuRatingService {

    @Autowired
    private MenuRatingMapper menuRatingMapper;
    @Autowired
    private OperationLogService logService;

    /**
     * 提交/更新评价
     */
    public MenuRating submitRating(User user, LocalDate date, Integer rating, String comment) {
        if (user.getFamilyId() == null) {
            throw new BizException("请先加入一个家庭哦~");
        }
        if (!"DINER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BizException("只有就餐者才能评价哦~");
        }

        // 检查是否14点后
        if (date.equals(LocalDate.now()) && LocalTime.now().isBefore(LocalTime.of(14, 0))) {
            throw new BizException("下午2点后才能评价今天的餐食哦，再等等~");
        }

        if (rating < 1 || rating > 5) {
            throw new BizException("评分范围是1-5星哦~");
        }

        // 查找已有评价
        MenuRating existing = menuRatingMapper.selectOne(
            new LambdaQueryWrapper<MenuRating>()
                .eq(MenuRating::getFamilyId, user.getFamilyId())
                .eq(MenuRating::getMenuDate, date)
                .eq(MenuRating::getUserId, user.getId())
        );

        if (existing != null) {
            existing.setRating(rating);
            existing.setComment(comment);
            menuRatingMapper.updateById(existing);
            logService.log(user, "RATING", "UPDATE", date,
                "修改了 " + date + " 的评价为 " + rating + " 星");
            return existing;
        } else {
            MenuRating menuRating = new MenuRating();
            menuRating.setFamilyId(user.getFamilyId());
            menuRating.setMenuDate(date);
            menuRating.setUserId(user.getId());
            menuRating.setRating(rating);
            menuRating.setComment(comment);
            menuRatingMapper.insert(menuRating);
            logService.log(user, "RATING", "CREATE", date,
                "为 " + date + " 的餐食评了 " + rating + " 星");
            return menuRating;
        }
    }

    /**
     * 获取某天的评价列表
     */
    public List<MenuRating> getRatingsByDate(Long familyId, LocalDate date) {
        return menuRatingMapper.selectList(
            new LambdaQueryWrapper<MenuRating>()
                .eq(MenuRating::getFamilyId, familyId)
                .eq(MenuRating::getMenuDate, date)
                .orderByDesc(MenuRating::getCreatedAt)
        );
    }

    /**
     * 获取最近N天的评价
     */
    public List<MenuRating> getRecentRatings(Long familyId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return menuRatingMapper.selectList(
            new LambdaQueryWrapper<MenuRating>()
                .eq(MenuRating::getFamilyId, familyId)
                .ge(MenuRating::getMenuDate, startDate)
                .orderByDesc(MenuRating::getMenuDate)
        );
    }

    /**
     * 获取用户对某天的评价
     */
    public MenuRating getUserRating(Long familyId, Long userId, LocalDate date) {
        return menuRatingMapper.selectOne(
            new LambdaQueryWrapper<MenuRating>()
                .eq(MenuRating::getFamilyId, familyId)
                .eq(MenuRating::getUserId, userId)
                .eq(MenuRating::getMenuDate, date)
        );
    }
}
