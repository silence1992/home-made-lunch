package com.homemadelunch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜谱标签关联实体
 */
@Data
@TableName("recipe_tag_relation")
public class RecipeTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜谱ID */
    private Long recipeId;

    /** 标签ID */
    private Long tagId;

    private LocalDateTime createdAt;
}
