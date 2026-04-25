package com.nexus.mapper;

import com.nexus.model.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车Mapper接口
 */
@Mapper
public interface CartMapper {

    /**
     * 插入购物车项
     */
    int insert(Cart cart);

    /**
     * 根据ID更新购物车项
     */
    int updateById(Cart cart);

    /**
     * 根据ID删除购物车项
     */
    int deleteById(Long id);

    /**
     * 根据会员ID和商品ID删除购物车项
     */
    int deleteByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    /**
     * 根据会员ID清空购物车
     */
    int deleteByMemberId(Long memberId);

    /**
     * 根据ID查询购物车项
     */
    Cart selectById(Long id);

    /**
     * 根据会员ID和商品ID查询购物车项
     */
    Cart selectByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    /**
     * 根据会员ID和商品ID查询购物车项（别名）
     */
    default Cart selectByMemberAndProduct(Long memberId, Long productId) {
        return selectByMemberIdAndProductId(memberId, productId);
    }

    /**
     * 根据会员ID查询购物车列表
     */
    List<Cart> selectByMemberId(Long memberId);

    /**
     * 统计会员购物车项数量
     */
    Long countByMemberId(Long memberId);
}