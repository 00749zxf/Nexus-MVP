-- Nexus电商系统Demo数据库脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS nexus_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nexus_demo;

-- 用户表 (ums_member)
CREATE TABLE IF NOT EXISTS ums_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(128) COMMENT '邮箱',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表 (pms_product)
CREATE TABLE IF NOT EXISTS pms_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    stock INT DEFAULT 0 COMMENT '库存数量',
    category_id BIGINT COMMENT '分类ID',
    status INT DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name),
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 购物车表 (oms_cart)
CREATE TABLE IF NOT EXISTS oms_cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT DEFAULT 1 COMMENT '购买数量',
    selected BOOLEAN DEFAULT true COMMENT '是否选中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_member_product (member_id, product_id),
    INDEX idx_member_id (member_id),
    FOREIGN KEY (member_id) REFERENCES ums_member(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES pms_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 订单表 (oms_order)
CREATE TABLE IF NOT EXISTS oms_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_sn VARCHAR(64) UNIQUE NOT NULL COMMENT '订单编号',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status INT DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    receiver_name VARCHAR(64) COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) COMMENT '收货人电话',
    receiver_address VARCHAR(200) COMMENT '收货地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_member_id (member_id),
    INDEX idx_order_sn (order_sn),
    INDEX idx_status (status),
    FOREIGN KEY (member_id) REFERENCES ums_member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单项表 (oms_order_item)
CREATE TABLE IF NOT EXISTS oms_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    product_price DECIMAL(10,2) COMMENT '商品单价',
    quantity INT NOT NULL COMMENT '购买数量',
    total_price DECIMAL(10,2) COMMENT '商品总价',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (order_id) REFERENCES oms_order(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES pms_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- 插入测试数据
-- 测试用户 (密码: admin123)
INSERT INTO ums_member (username, password, phone, email, status) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJI6Au2e', '13800138000', 'admin@nexus.com', 1),
('user1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJI6Au2e', '13800138001', 'user1@nexus.com', 1);

-- 测试商品
INSERT INTO pms_product (name, description, price, stock, category_id, status) VALUES
('iPhone 15 Pro', '苹果最新款智能手机', 8999.00, 100, 1, 1),
('MacBook Air M3', '苹果轻薄笔记本电脑', 9999.00, 50, 2, 1),
('AirPods Pro', '苹果降噪无线耳机', 1999.00, 200, 3, 1),
('iPad Pro', '苹果平板电脑', 6999.00, 80, 4, 1);

-- 测试购物车
INSERT INTO oms_cart (member_id, product_id, quantity, selected) VALUES
(1, 1, 2, true),
(1, 2, 1, true),
(2, 3, 3, true);