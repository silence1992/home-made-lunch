package com.homemadelunch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 食谱评价实体
 */
@Data
@TableName("menu_rating")
public class MenuRating {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属家庭ID */
    private Long familyId;

    /** 评价日期 */
    private LocalDate menuDate;

    /** 评价人ID */
    private Long userId;

    /** 评分(1-5星) */
    private Integer rating;

    /** 评价内容 */
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
