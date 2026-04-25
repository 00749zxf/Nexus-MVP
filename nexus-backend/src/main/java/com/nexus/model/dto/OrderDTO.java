package com.nexus.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建订单DTO
 */
@Data
public class OrderDTO {

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;

    /**
     * 购物车项ID列表（从购物车创建订单）
     */
    private List<Long> cartItemIds;

    /**
     * 商品ID（直接购买）
     */
    private Long productId;

    /**
     * 购买数量（直接购买）
     */
    private Integer quantity;

    /**
     * 备注
     */
    private String note;
}