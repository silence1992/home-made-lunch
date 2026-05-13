package com.homemadelunch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邀请链接实体
 */
@Data
@TableName("invite_link")
public class InviteLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭ID */
    private Long familyId;

    /** 邀请码 */
    private String code;

    /** 邀请角色 */
    private String role;

    /** 创建人ID */
    private Long createdBy;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** 是否已使用 */
    private Integer used;

    /** 使用人ID */
    private Long usedBy;

    private LocalDateTime createdAt;
}
