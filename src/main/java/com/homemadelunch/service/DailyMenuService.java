package com.homemadelunch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homemadelunch.common.BizException;
import com.homemadelunch.entity.*;
import com.homemadelunch.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日食谱服务
 */
@Slf4j
@Service
public class DailyMenuService {

    @Autowired
    private DailyMenuMapper dailyMenuMapper;
    @Autowired
    private RecipeMapper recipeMapper;
    @Autowired
    private RecipeTagMapper recipeTagMapper;
    @Autowired
    private RecipeTagRelationMapper recipeTagRelationMapper;
    @Autowired
    private OperationLogService logService;

    /**
     * 获取本周工作日食谱
     */
    public List<Map<String, Object>> getWeeklyMenu(Long familyId, LocalDate baseDate) {
        // 计算本周一和周五
        LocalDate monday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        List<Map<String, Object>> weekMenu = new ArrayList<>();

        for (LocalDate date = monday; !date.isAfter(friday); date = date.plusDays(1)) {
            Map<String, Object> dayMenu = new LinkedHashMap<>();
            dayMenu.put("date", date.toString());
            dayMenu.put("dayOfWeek", getDayOfWeekChinese(date.getDayOfWeek()));
            dayMenu.put("isToday", date.equals(LocalDate.now()));

            // 查询当天菜谱
            List<DailyMenu> menus = dailyMenuMapper.selectList(
                new LambdaQueryWrapper<DailyMenu>()
                    .eq(DailyMenu::getFamilyId, familyId)
                    .eq(DailyMenu::getMenuDate, date)
                    .orderByAsc(DailyMenu::getSortOrder)
            );

            if (menus.isEmpty()) {
                // 默认为"随意"
                Recipe defaultRecipe = getDefaultRecipe(familyId, "随意");
                List<Map<String, Object>> recipes = new ArrayList<>();
                if (defaultRecipe != null) {
                    Map<String, Object> recipeMap = buildRecipeMap(defaultRecipe);
                    recipeMap.put("isDefault", true);
                    recipes.add(recipeMap);
                }
                dayMenu.put("recipes", recipes);
                dayMenu.put("isDefaultMenu", true);
            } else {
                List<Map<String, Object>> recipes = menus.stream().map(menu -> {
                    Recipe recipe = recipeMapper.selectById(menu.getRecipeId());
                    Map<String, Object> recipeMap = buildRecipeMap(recipe);
                    recipeMap.put("menuId", menu.getId());
                    return recipeMap;
                }).collect(Collectors.toList());
                dayMenu.put("recipes", recipes);

                // 判断是否为默认菜谱
                boolean isDefault = menus.size() == 1 &&
                    recipeMapper.selectById(menus.get(0).getRecipeId()).getIsDefault() == 1;
                dayMenu.put("isDefaultMenu", isDefault);
            }

            weekMenu.add(dayMenu);
        }

        return weekMenu;
    }

    /**
     * 构建菜谱信息Map
     */
    private Map<String, Object> buildRecipeMap(Recipe recipe) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (recipe != null) {
            map.put("recipeId", recipe.getId());
            map.put("recipeName", recipe.getName());
            map.put("description", recipe.getDescription());
            map.put("isDefault", recipe.getIsDefault() == 1);

            // 查询标签
            List<RecipeTagRelation> relations = recipeTagRelationMapper.selectList(
                new LambdaQueryWrapper<RecipeTagRelation>()
                    .eq(RecipeTagRelation::getRecipeId, recipe.getId())
            );
            if (!relations.isEmpty()) {
                List<Long> tagIds = relations.stream()
                    .map(RecipeTagRelation::getTagId).collect(Collectors.toList());
                List<RecipeTag> tags = recipeTagMapper.selectBatchIds(tagIds);
                map.put("tags", tags.stream().map(RecipeTag::getName).collect(Collectors.toList()));
            } else {
                map.put("tags", Collections.emptyList());
            }
        }
        return map;
    }

    /**
     * 设置某天的菜谱
     */
    @Transactional
    public void setDayMenu(User user, LocalDate date, List<Long> recipeIds) {
        if (user.getFamilyId() == null) {
            throw new BizException("请先加入一个家庭哦~");
        }

        Long familyId = user.getFamilyId();

        // 检查是否包含默认菜谱
        List<Recipe> recipes = new ArrayList<>();
        boolean hasDefault = false;
        for (Long recipeId : recipeIds) {
            Recipe recipe = recipeMapper.selectById(recipeId);
            if (recipe == null || !recipe.getFamilyId().equals(familyId)) {
                throw new BizException("菜谱不存在~");
            }
            recipes.add(recipe);
            if (recipe.getIsDefault() == 1) {
                hasDefault = true;
            }
        }

        // 默认菜谱不能和其它菜谱共存
        if (hasDefault && recipeIds.size() > 1) {
            throw new BizException("「随意」和「停休」不能和其它菜谱一起选哦~");
        }

        // 如果选择了默认菜谱，清空当日所有菜谱后设置
        // 先删除当天所有菜谱
        dailyMenuMapper.delete(
            new LambdaQueryWrapper<DailyMenu>()
                .eq(DailyMenu::getFamilyId, familyId)
                .eq(DailyMenu::getMenuDate, date)
        );

        // 添加新菜谱
        for (int i = 0; i < recipeIds.size(); i++) {
            DailyMenu menu = new DailyMenu();
            menu.setFamilyId(familyId);
            menu.setMenuDate(date);
            menu.setRecipeId(recipeIds.get(i));
            menu.setSortOrder(i);
            menu.setCreatedBy(user.getId());
            dailyMenuMapper.insert(menu);
        }

        String recipeNames = recipes.stream()
            .map(Recipe::getName).collect(Collectors.joining("、"));
        logService.log(user, "MENU", "UPDATE", date,
            "设置了 " + date + " 的菜谱为: " + recipeNames);
    }

    /**
     * 添加菜谱到某天
     */
    @Transactional
    public void addRecipeToDay(User user, LocalDate date, Long recipeId) {
        if (user.getFamilyId() == null) {
            throw new BizException("请先加入一个家庭哦~");
        }

        Long familyId = user.getFamilyId();
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || !recipe.getFamilyId().equals(familyId)) {
            throw new BizException("菜谱不存在~");
        }

        // 查询当天已有菜谱
        List<DailyMenu> existingMenus = dailyMenuMapper.selectList(
            new LambdaQueryWrapper<DailyMenu>()
                .eq(DailyMenu::getFamilyId, familyId)
                .eq(DailyMenu::getMenuDate, date)
        );

        // 如果新增的是默认菜谱，清空其它所有
        if (recipe.getIsDefault() == 1) {
            dailyMenuMapper.delete(
                new LambdaQueryWrapper<DailyMenu>()
                    .eq(DailyMenu::getFamilyId, familyId)
                    .eq(DailyMenu::getMenuDate, date)
            );
            DailyMenu menu = new DailyMenu();
            menu.setFamilyId(familyId);
            menu.setMenuDate(date);
            menu.setRecipeId(recipeId);
            menu.setSortOrder(0);
            menu.setCreatedBy(user.getId());
            dailyMenuMapper.insert(menu);

            logService.log(user, "MENU", "UPDATE", date,
                "将 " + date + " 的菜谱设为「" + recipe.getName() + "」");
            return;
        }

        // 如果当天已有默认菜谱，先清空
        for (DailyMenu existing : existingMenus) {
            Recipe existRecipe = recipeMapper.selectById(existing.getRecipeId());
            if (existRecipe != null && existRecipe.getIsDefault() == 1) {
                dailyMenuMapper.delete(
                    new LambdaQueryWrapper<DailyMenu>()
                        .eq(DailyMenu::getFamilyId, familyId)
                        .eq(DailyMenu::getMenuDate, date)
                );
                existingMenus.clear();
                break;
            }
        }

        // 检查是否已添加过
        boolean alreadyExists = existingMenus.stream()
            .anyMatch(m -> m.getRecipeId().equals(recipeId));
        if (alreadyExists) {
            throw new BizException("这道菜已经在今天的菜单里啦~");
        }

        int maxSort = existingMenus.stream()
            .mapToInt(DailyMenu::getSortOrder).max().orElse(-1);

        DailyMenu menu = new DailyMenu();
        menu.setFamilyId(familyId);
        menu.setMenuDate(date);
        menu.setRecipeId(recipeId);
        menu.setSortOrder(maxSort + 1);
        menu.setCreatedBy(user.getId());
        dailyMenuMapper.insert(menu);

        logService.log(user, "MENU", "ADD", date,
            "为 " + date + " 添加了菜谱「" + recipe.getName() + "」");
    }

    /**
     * 从某天移除菜谱
     */
    @Transactional
    public void removeRecipeFromDay(User user, LocalDate date, Long recipeId) {
        if (user.getFamilyId() == null) {
            throw new BizException("请先加入一个家庭哦~");
        }

        Long familyId = user.getFamilyId();
        Recipe recipe = recipeMapper.selectById(recipeId);

        dailyMenuMapper.delete(
            new LambdaQueryWrapper<DailyMenu>()
                .eq(DailyMenu::getFamilyId, familyId)
                .eq(DailyMenu::getMenuDate, date)
                .eq(DailyMenu::getRecipeId, recipeId)
        );

        // 如果当天没有菜谱了，恢复为"随意"
        Long count = dailyMenuMapper.selectCount(
            new LambdaQueryWrapper<DailyMenu>()
                .eq(DailyMenu::getFamilyId, familyId)
                .eq(DailyMenu::getMenuDate, date)
        );
        if (count == 0) {
            Recipe defaultRecipe = getDefaultRecipe(familyId, "随意");
            if (defaultRecipe != null) {
                DailyMenu menu = new DailyMenu();
                menu.setFamilyId(familyId);
                menu.setMenuDate(date);
                menu.setRecipeId(defaultRecipe.getId());
                menu.setSortOrder(0);
                menu.setCreatedBy(user.getId());
                dailyMenuMapper.insert(menu);
            }
        }

        String recipeName = recipe != null ? recipe.getName() : "未知菜谱";
        logService.log(user, "MENU", "REMOVE", date,
            "从 " + date + " 移除了菜谱「" + recipeName + "」");
    }

    /**
     * 获取某天的菜谱列表
     */
    public List<Map<String, Object>> getDayMenu(Long familyId, LocalDate date) {
        List<DailyMenu> menus = dailyMenuMapper.selectList(
            new LambdaQueryWrapper<DailyMenu>()
                .eq(DailyMenu::getFamilyId, familyId)
                .eq(DailyMenu::getMenuDate, date)
                .orderByAsc(DailyMenu::getSortOrder)
        );

        if (menus.isEmpty()) {
            Recipe defaultRecipe = getDefaultRecipe(familyId, "随意");
            if (defaultRecipe != null) {
                Map<String, Object> recipeMap = buildRecipeMap(defaultRecipe);
                recipeMap.put("isDefault", true);
                return Collections.singletonList(recipeMap);
            }
            return Collections.emptyList();
        }

        return menus.stream().map(menu -> {
            Recipe recipe = recipeMapper.selectById(menu.getRecipeId());
            Map<String, Object> recipeMap = buildRecipeMap(recipe);
            recipeMap.put("menuId", menu.getId());
            return recipeMap;
        }).collect(Collectors.toList());
    }

    /**
     * 获取默认菜谱
     */
    public Recipe getDefaultRecipe(Long familyId, String name) {
        return recipeMapper.selectOne(
            new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getFamilyId, familyId)
                .eq(Recipe::getName, name)
                .eq(Recipe::getIsDefault, 1)
        );
    }

    private String getDayOfWeekChinese(DayOfWeek dow) {
        switch (dow) {
            case MONDAY: return "周一";
            case TUESDAY: return "周二";
            case WEDNESDAY: return "周三";
            case THURSDAY: return "周四";
            case FRIDAY: return "周五";
            case SATURDAY: return "周六";
            case SUNDAY: return "周日";
            default: return "";
        }
    }
}
