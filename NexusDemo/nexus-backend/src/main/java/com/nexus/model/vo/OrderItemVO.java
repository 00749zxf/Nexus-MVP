package com.nexus.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项视图对象
 */
@Data
public class OrderItemVO {

    /**
     * 订单项ID
     */
    private Long id;

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
     * 商品图片
     */
    private String productImage;
}