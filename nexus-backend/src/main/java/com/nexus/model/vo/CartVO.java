package com.nexus.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 购物车视图对象
 */
@Data
public class CartVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     */
    private Double productPrice;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 是否选中
     */
    private Boolean selected;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 小计金额
     */
    private Double subtotal;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 是否可用（库存是否充足）
     */
    private Boolean available;
}