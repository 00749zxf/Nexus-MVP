package com.nexus.agent.core;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * Agent上下文信息
 */
@Data
public class AgentContext implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前登录用户ID
     */
    private Long memberId;

    /**
     * 当前用户名
     */
    private String username;

    /**
     * 当前页面路径
     */
    private String currentPage;

    /**
     * 当前商品ID（如果在商品详情页）
     */
    private Long currentProductId;

    /**
     * 当前订单ID（如果在订单详情页）
     */
    private Long currentOrderId;

    /**
     * 扩展上下文
     */
    private Map<String, Object> extra;
}