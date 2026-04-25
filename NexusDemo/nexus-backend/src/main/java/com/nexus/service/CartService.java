package com.nexus.service;

import com.nexus.model.dto.CartDTO;
import com.nexus.model.vo.CartVO;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 获取当前用户的购物车列表
     */
    List<CartVO> getCurrentUserCart();

    /**
     * 添加商品到购物车
     */
    Long addToCart(CartDTO cartDTO);

    /**
     * 更新购物车商品数量
     */
    void updateQuantity(Long cartItemId, Integer quantity);

    /**
     * 删除购物车商品
     */
    void removeCartItem(Long cartItemId);

    /**
     * 清空当前用户购物车
     */
    void clearCart();

    /**
     * 更新商品选择状态
     */
    void updateSelection(Long cartItemId, Boolean selected);

    /**
     * 批量更新选择状态
     */
    void batchUpdateSelection(List<Long> cartItemIds, Boolean selected);
}