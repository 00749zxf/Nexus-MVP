-- =============================================
-- NexusDemo Database Migration Script
-- Version: 1.0.1
-- Description: Add favorites and address tables, add avatar to member
-- =============================================

-- 1. 添加收藏表 (ums_favorite)
CREATE TABLE IF NOT EXISTS ums_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_member_product (member_id, product_id) COMMENT '防止重复收藏',
    INDEX idx_member_id (member_id) COMMENT '会员查询索引',
    INDEX idx_product_id (product_id) COMMENT '商品查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 2. 添加收货地址表 (ums_address)
CREATE TABLE IF NOT EXISTS ums_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区/县',
    detail_address VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认地址',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_member_id (member_id) COMMENT '会员查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

-- 3. Member表添加avatar字段
ALTER TABLE ums_member ADD COLUMN IF NOT EXISTS avatar VARCHAR(255) DEFAULT NULL COMMENT '用户头像URL' AFTER email;

-- =============================================
-- End of Migration Script
-- =============================================