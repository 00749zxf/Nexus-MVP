package com.nexus.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单项实体类
 * 对应表: oms_order_item
 */
@Data
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品单价
     */
    private BigDecimal productPrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 商品总价
     */
    private BigDecimal totalPrice;

    /**
     * 创建时间
     */
    private Date createTime;
}