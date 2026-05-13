package com.homemadelunch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homemadelunch.common.BizException;
import com.homemadelunch.entity.*;
import com.homemadelunch.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜谱服务
 */
@Slf4j
@Service
public class RecipeService {

    @Autowired
    private RecipeMapper recipeMapper;
    @Autowired
    private RecipeTagMapper recipeTagMapper;
    @Autowired
    private RecipeTagRelationMapper recipeTagRelationMapper;
    @Autowired
    private OperationLogService logService;

    // ==================== 菜谱标签管理 ====================

    /**
     * 获取家庭所有标签
     */
    public List<RecipeTag> getTagsByFamily(Long familyId) {
        return recipeTagMapper.selectList(
            new LambdaQueryWrapper<RecipeTag>()
                .eq(RecipeTag::getFamilyId, familyId)
                .orderByAsc(RecipeTag::getIsDefault)
                .orderByAsc(RecipeTag::getId)
        );
    }

    /**
     * 创建标签
     */
    public RecipeTag createTag(User user, String name) {
        if (user.getFamilyId() == null) {
            throw new BizException("请先加入一个家庭哦~");
        }

        // 检查名称重复
        Long count = recipeTagMapper.selectCount(
            new LambdaQueryWrapper<RecipeTag>()
                .eq(RecipeTag::getFamilyId, user.getFamilyId())
                .eq(RecipeTag::getName, name)
        );
        if (count > 0) {
            throw new BizException("标签「" + name + "」已经存在啦~");
        }

        RecipeTag tag = new RecipeTag();
        tag.setFamilyId(user.getFamilyId());
        tag.setName(name);
        tag.setIsDefault(0);
        recipeTagMapper.insert(tag);

        logService.log(user, "TAG", "CREATE", "创建了标签「" + name + "」");
        return tag;
    }

    /**
     * 更新标签
     */
    public RecipeTag updateTag(User user, Long tagId, String name) {
        RecipeTag tag = recipeTagMapper.selectById(tagId);
        if (tag == null || !tag.getFamilyId().equals(user.getFamilyId())) {
            throw new BizException("标签不存在~");
        }
        if (tag.getIsDefault() == 1) {
            throw new BizException("默认标签不能修改哦~");
        }

        // 检查名称重复
        Long count = recipeTagMapper.selectCount(
            new LambdaQueryWrapper<RecipeTag>()
                .eq(RecipeTag::getFamilyId, user.getFamilyId())
                .eq(RecipeTag::getName, name)
                .ne(RecipeTag::getId, tagId)
        );
        if (count > 0) {
            throw new BizException("标签「" + name + "」已经存在啦~");
        }

        String oldName = tag.getName();
        tag.setName(name);
        recipeTagMapper.updateById(tag);

        logService.log(user, "TAG", "UPDATE", "将标签「" + oldName + "」改为「" + name + "」");
        return tag;
    }

    /**
     * 删除标签
     */
    public void deleteTag(User user, Long tagId) {
        RecipeTag tag = recipeTagMapper.selectById(tagId);
        if (tag == null || !tag.getFamilyId().equals(user.getFamilyId())) {
            throw new BizException("标签不存在~");
        }
        if (tag.getIsDefault() == 1) {
            throw new BizException("默认标签不能删除哦~");
        }

        // 删除关联关系
        recipeTagRelationMapper.delete(
            new LambdaQueryWrapper<RecipeTagRelation>()
                .eq(RecipeTagRelation::getTagId, tagId)
        );
        recipeTagMapper.deleteById(tagId);

        logService.log(user, "TAG", "DELETE", "删除了标签「" + tag.getName() + "」");
    }

    // ==================== 菜谱管理 ====================

    /**
     * 获取家庭所有菜谱
     */
    public List<Map<String, Object>> getRecipesByFamily(Long familyId) {
        List<Recipe> recipes = recipeMapper.selectList(
            new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getFamilyId, familyId)
                .orderByDesc(Recipe::getIsDefault)
                .orderByDesc(Recipe::getId)
        );

        return recipes.stream().map(recipe -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", recipe.getId());
            map.put("name", recipe.getName());
            map.put("description", recipe.getDescription());
            map.put("isDefault", recipe.getIsDefault());
            map.put("createdAt", recipe.getCreatedAt());

            // 查询关联标签
            List<RecipeTagRelation> relations = recipeTagRelationMapper.selectList(
                new LambdaQueryWrapper<RecipeTagRelation>()
                    .eq(RecipeTagRelation::getRecipeId, recipe.getId())
            );
            if (!relations.isEmpty()) {
                List<Long> tagIds = relations.stream()
                    .map(RecipeTagRelation::getTagId).collect(Collectors.toList());
                List<RecipeTag> tags = recipeTagMapper.selectBatchIds(tagIds);
                map.put("tags", tags);
            } else {
                map.put("tags", Collections.emptyList());
            }
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 创建菜谱
     */
    @Transactional
    public Recipe createRecipe(User user, String name, String description, List<Long> tagIds) {
        if (user.getFamilyId() == null) {
            throw new BizException("请先加入一个家庭哦~");
        }

        // 检查名称重复
        Long count = recipeMapper.selectCount(
            new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getFamilyId, user.getFamilyId())
                .eq(Recipe::getName, name)
        );
        if (count > 0) {
            throw new BizException("菜谱「" + name + "」已经存在啦~");
        }

        Recipe recipe = new Recipe();
        recipe.setFamilyId(user.getFamilyId());
        recipe.setName(name);
        recipe.setDescription(description);
        recipe.setIsDefault(0);
        recipe.setCreatedBy(user.getId());
        recipeMapper.insert(recipe);

        // 绑定标签
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                RecipeTagRelation rel = new RecipeTagRelation();
                rel.setRecipeId(recipe.getId());
                rel.setTagId(tagId);
                recipeTagRelationMapper.insert(rel);
            }
        }

        logService.log(user, "RECIPE", "CREATE", "创建了菜谱「" + name + "」");
        return recipe;
    }

    /**
     * 更新菜谱
     */
    @Transactional
    public Recipe updateRecipe(User user, Long recipeId, String name, String description, List<Long> tagIds) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || !recipe.getFamilyId().equals(user.getFamilyId())) {
            throw new BizException("菜谱不存在~");
        }
        if (recipe.getIsDefault() == 1) {
            throw new BizException("默认菜谱不能修改哦~");
        }

        // 检查名称重复
        if (name != null && !name.equals(recipe.getName())) {
            Long count = recipeMapper.selectCount(
                new LambdaQueryWrapper<Recipe>()
                    .eq(Recipe::getFamilyId, user.getFamilyId())
                    .eq(Recipe::getName, name)
                    .ne(Recipe::getId, recipeId)
            );
            if (count > 0) {
                throw new BizException("菜谱「" + name + "」已经存在啦~");
            }
            recipe.setName(name);
        }

        if (description != null) {
            recipe.setDescription(description);
        }
        recipeMapper.updateById(recipe);

        // 更新标签关联
        if (tagIds != null) {
            recipeTagRelationMapper.delete(
                new LambdaQueryWrapper<RecipeTagRelation>()
                    .eq(RecipeTagRelation::getRecipeId, recipeId)
            );
            for (Long tagId : tagIds) {
                RecipeTagRelation rel = new RecipeTagRelation();
                rel.setRecipeId(recipeId);
                rel.setTagId(tagId);
                recipeTagRelationMapper.insert(rel);
            }
        }

        logService.log(user, "RECIPE", "UPDATE", "更新了菜谱「" + recipe.getName() + "」");
        return recipe;
    }

    /**
     * 删除菜谱
     */
    @Transactional
    public void deleteRecipe(User user, Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || !recipe.getFamilyId().equals(user.getFamilyId())) {
            throw new BizException("菜谱不存在~");
        }
        if (recipe.getIsDefault() == 1) {
            throw new BizException("默认菜谱不能删除哦~");
        }

        recipeTagRelationMapper.delete(
            new LambdaQueryWrapper<RecipeTagRelation>()
                .eq(RecipeTagRelation::getRecipeId, recipeId)
        );
        recipeMapper.deleteById(recipeId);

        logService.log(user, "RECIPE", "DELETE", "删除了菜谱「" + recipe.getName() + "」");
    }

    /**
     * 根据ID获取菜谱
     */
    public Recipe getRecipeById(Long recipeId) {
        return recipeMapper.selectById(recipeId);
    }
}
