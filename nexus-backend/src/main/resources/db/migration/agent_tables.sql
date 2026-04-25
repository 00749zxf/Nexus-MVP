-- Agent相关数据库表

-- Agent会话表
CREATE TABLE IF NOT EXISTS agent_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    member_id BIGINT COMMENT '用户ID',
    agent_type VARCHAR(32) NOT NULL COMMENT 'Agent类型',
    role VARCHAR(16) NOT NULL COMMENT '角色：USER/AGENT',
    content TEXT NOT NULL COMMENT '消息内容',
    tool_calls JSON COMMENT '工具调用记录',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session (session_id),
    INDEX idx_member (member_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

-- 用户行为事件表
CREATE TABLE IF NOT EXISTS ums_user_behavior (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_id BIGINT NOT NULL COMMENT '用户ID',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型：VIEW/ADD_CART/ORDER/PAY/SEARCH',
    target_id BIGINT COMMENT '目标ID（商品ID/订单ID等）',
    event_data JSON COMMENT '事件详情数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_member_event (member_id, event_type),
    INDEX idx_created (created_at),
    INDEX idx_target (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为事件表';

-- 用户偏好向量表
CREATE TABLE IF NOT EXISTS rec_user_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    member_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    category_preference JSON COMMENT '分类偏好',
    brand_preference JSON COMMENT '品牌偏好',
    price_range VARCHAR(64) COMMENT '价格偏好区间',
    recent_keywords JSON COMMENT '最近搜索关键词',
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好向量表';

-- FAQ知识库表
CREATE TABLE IF NOT EXISTS knowledge_faq (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category VARCHAR(64) NOT NULL COMMENT '分类',
    question VARCHAR(500) NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '答案',
    keywords JSON COMMENT '关键词',
    embedding_vector JSON COMMENT '向量（可选，用于相似度检索）',
    priority INT DEFAULT 0 COMMENT '优先级',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FAQ知识库表';

-- 插入示例FAQ数据
INSERT INTO knowledge_faq (category, question, answer, keywords, priority, status) VALUES
('售后', '如何申请退款？', '您可以在订单详情页面点击"申请退款"按钮。退款申请提交后，我们会在1-3个工作日内处理。退款成功后，款项将退回到您的原支付账户。', '["退款", "退货", "申请"]', 10, 1),
('售后', '退换货政策是什么？', '我们支持7天无理由退换货。商品需要保持原包装完好，不影响二次销售。具体流程：1. 提交退换申请 2. 等待审核 3. 寄回商品 4. 审核通过后退款或换货。', '["退换货", "7天", "政策"]', 10, 1),
('发货', '订单什么时候发货？', '订单支付成功后，通常会在24小时内发货。发货后您会收到短信通知，可以在订单详情查看物流信息。', '["发货", "物流", "时间"]', 9, 1),
('发货', '可以指定快递公司吗？', '目前我们不支持指定快递公司。我们会根据收货地址选择最优的快递服务，确保商品安全送达。', '["快递", "指定", "物流"]', 5, 1),
('支付', '支持哪些支付方式？', '我们支持微信支付、支付宝、银行卡等多种支付方式。具体可选支付方式会在结算页面显示。', '["支付", "微信", "支付宝"]', 8, 1),
('支付', '支付失败怎么办？', '支付失败可能原因：1. 网络问题，请重试 2. 余额不足 3. 银行卡问题。建议检查后重新支付，或联系客服协助。', '["支付失败", "问题"]', 7, 1),
('商品', '商品质量有问题怎么办？', '如收到商品有质量问题，请在7天内提交售后申请，并提供照片证明。我们会安排退换货处理。', '["质量问题", "售后", "退换"]', 8, 1),
('账户', '如何修改密码？', '登录后进入"个人中心" -> "账户安全" -> "修改密码"，输入原密码和新密码即可修改。', '["密码", "修改", "安全"]', 6, 1);