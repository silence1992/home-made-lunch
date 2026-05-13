-- 家庭便当数据库初始化脚本
CREATE DATABASE IF NOT EXISTS home_made_lunch DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE home_made_lunch;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `openid` VARCHAR(128) NOT NULL UNIQUE COMMENT '微信openid',
    `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `family_id` BIGINT DEFAULT NULL COMMENT '所属家庭ID',
    `role` VARCHAR(20) DEFAULT NULL COMMENT '家庭角色: ADMIN/COOK/DINER',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_openid (`openid`),
    INDEX idx_family_id (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 家庭表
CREATE TABLE IF NOT EXISTS `family` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '家庭ID',
    `name` VARCHAR(64) NOT NULL COMMENT '家庭名称',
    `admin_id` BIGINT NOT NULL COMMENT '管理员用户ID',
    `invite_code` VARCHAR(32) NOT NULL UNIQUE COMMENT '邀请码',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_admin_id (`admin_id`),
    INDEX idx_invite_code (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭表';

-- 菜谱标签表
CREATE TABLE IF NOT EXISTS `recipe_tag` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID',
    `name` VARCHAR(64) NOT NULL COMMENT '标签名称',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认标签(不可删改)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_family_name (`family_id`, `name`, `deleted`),
    INDEX idx_family_id (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱标签表';

-- 菜谱表
CREATE TABLE IF NOT EXISTS `recipe` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜谱ID',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID',
    `name` VARCHAR(128) NOT NULL COMMENT '菜谱名称',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '菜谱描述',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认菜谱(不可删改)',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_family_name (`family_id`, `name`, `deleted`),
    INDEX idx_family_id (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱表';

-- 菜谱-标签关联表
CREATE TABLE IF NOT EXISTS `recipe_tag_relation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_recipe_tag (`recipe_id`, `tag_id`),
    INDEX idx_recipe_id (`recipe_id`),
    INDEX idx_tag_id (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱标签关联表';

-- 每日食谱表（工作日菜单）
CREATE TABLE IF NOT EXISTS `daily_menu` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID',
    `menu_date` DATE NOT NULL COMMENT '日期',
    `recipe_id` BIGINT NOT NULL COMMENT '菜谱ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_family_date (`family_id`, `menu_date`),
    INDEX idx_recipe_id (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日食谱表';

-- 食谱评价表
CREATE TABLE IF NOT EXISTS `menu_rating` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `family_id` BIGINT NOT NULL COMMENT '所属家庭ID',
    `menu_date` DATE NOT NULL COMMENT '评价日期',
    `user_id` BIGINT NOT NULL COMMENT '评价人ID',
    `rating` INT NOT NULL DEFAULT 5 COMMENT '评分(1-5星)',
    `comment` VARCHAR(512) DEFAULT NULL COMMENT '评价内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_family_date_user (`family_id`, `menu_date`, `user_id`, `deleted`),
    INDEX idx_family_date (`family_id`, `menu_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食谱评价表';

-- 操作记录表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `family_id` BIGINT DEFAULT NULL COMMENT '所属家庭ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `user_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人昵称',
    `module` VARCHAR(32) NOT NULL COMMENT '模块: USER/FAMILY/RECIPE/TAG/MENU/RATING',
    `action` VARCHAR(32) NOT NULL COMMENT '操作: CREATE/UPDATE/DELETE/JOIN/LEAVE/KICK',
    `target_date` DATE DEFAULT NULL COMMENT '关联日期(食谱相关)',
    `content` VARCHAR(1024) DEFAULT NULL COMMENT '操作内容描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_family_id (`family_id`),
    INDEX idx_target_date (`family_id`, `target_date`),
    INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作记录表';

-- 邀请链接表
CREATE TABLE IF NOT EXISTS `invite_link` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `family_id` BIGINT NOT NULL COMMENT '家庭ID',
    `code` VARCHAR(64) NOT NULL UNIQUE COMMENT '邀请码',
    `role` VARCHAR(20) NOT NULL COMMENT '邀请角色: COOK/DINER',
    `created_by` BIGINT NOT NULL COMMENT '创建人ID',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `used` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用',
    `used_by` BIGINT DEFAULT NULL COMMENT '使用人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_code (`code`),
    INDEX idx_family_id (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请链接表';
