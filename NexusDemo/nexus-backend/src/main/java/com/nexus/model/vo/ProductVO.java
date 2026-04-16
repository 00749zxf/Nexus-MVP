package com.nexus.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品视图对象
 */
@Data
public class ProductVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String image;
    private Integer stock;
    private Integer sales;
    private String category;
    private Double rating;
    private Integer reviews;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}