package com.homemadelunch.controller;

import com.homemadelunch.common.Result;
import com.homemadelunch.entity.*;
import com.homemadelunch.service.RecipeService;
import com.homemadelunch.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 菜谱控制器
 */
@Slf4j
@RestController
@RequestMapping("/recipe")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    // ==================== 标签管理 ====================

    /**
     * 获取标签列表
     */
    @GetMapping("/tags")
    public Result<List<RecipeTag>> getTags(HttpServletRequest request) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        List<RecipeTag> tags = recipeService.getTagsByFamily(user.getFamilyId());
        return Result.ok(tags);
    }

    /**
     * 创建标签
     */
    @PostMapping("/tag/create")
    public Result<RecipeTag> createTag(HttpServletRequest request, @RequestBody Map<String, String> params) {
        User user = UserContext.getCurrentUser(request);
        String name = params.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("标签名称不能为空哦~");
        }
        RecipeTag tag = recipeService.createTag(user, name.trim());
        return Result.ok("标签创建成功~", tag);
    }

    /**
     * 更新标签
     */
    @PutMapping("/tag/{id}")
    public Result<RecipeTag> updateTag(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, String> params) {
        User user = UserContext.getCurrentUser(request);
        String name = params.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("标签名称不能为空哦~");
        }
        RecipeTag tag = recipeService.updateTag(user, id, name.trim());
        return Result.ok("标签更新成功~", tag);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/tag/{id}")
    public Result<?> deleteTag(HttpServletRequest request, @PathVariable Long id) {
        User user = UserContext.getCurrentUser(request);
        recipeService.deleteTag(user, id);
        return Result.ok("标签已删除~");
    }

    // ==================== 菜谱管理 ====================

    /**
     * 获取菜谱列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getRecipes(HttpServletRequest request) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("请先加入一个家庭哦~");
        }
        List<Map<String, Object>> recipes = recipeService.getRecipesByFamily(user.getFamilyId());
        return Result.ok(recipes);
    }

    /**
     * 创建菜谱
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/create")
    public Result<Recipe> createRecipe(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        String name = (String) params.get("name");
        String description = (String) params.get("description");
        List<Long> tagIds = null;
        if (params.get("tagIds") != null) {
            tagIds = ((List<Number>) params.get("tagIds")).stream()
                .map(Number::longValue).collect(java.util.stream.Collectors.toList());
        }
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("菜谱名称不能为空哦~");
        }
        Recipe recipe = recipeService.createRecipe(user, name.trim(), description, tagIds);
        return Result.ok("菜谱创建成功~", recipe);
    }

    /**
     * 更新菜谱
     */
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public Result<Recipe> updateRecipe(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        String name = (String) params.get("name");
        String description = (String) params.get("description");
        List<Long> tagIds = null;
        if (params.get("tagIds") != null) {
            tagIds = ((List<Number>) params.get("tagIds")).stream()
                .map(Number::longValue).collect(java.util.stream.Collectors.toList());
        }
        Recipe recipe = recipeService.updateRecipe(user, id, name, description, tagIds);
        return Result.ok("菜谱更新成功~", recipe);
    }

    /**
     * 删除菜谱
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteRecipe(HttpServletRequest request, @PathVariable Long id) {
        User user = UserContext.getCurrentUser(request);
        recipeService.deleteRecipe(user, id);
        return Result.ok("菜谱已删除~");
    }
}
