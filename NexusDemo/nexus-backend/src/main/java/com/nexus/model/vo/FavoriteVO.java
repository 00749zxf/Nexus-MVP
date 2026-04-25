package com.nexus.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收藏商品VO
 */
@Data
public class FavoriteVO {

    /**
     * 收藏ID
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
    private BigDecimal productPrice;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 商品库存
     */
    private Integer productStock;

    /**
     * 商品是否可用
     */
    private Boolean productAvailable;

    /**
     * 收藏时间
     */
    private Date createTime;
}