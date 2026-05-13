package com.homemadelunch.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作记录实体
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属家庭ID */
    private Long familyId;

    /** 操作人ID */
    private Long userId;

    /** 操作人昵称 */
    private String userName;

    /** 模块 */
    private String module;

    /** 操作 */
    private String action;

    /** 关联日期 */
    private LocalDate targetDate;

    /** 操作内容描述 */
    private String content;

    private LocalDateTime createdAt;
}
