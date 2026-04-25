package com.nexus.mapper;

import com.nexus.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper {

    /**
     * 插入订单
     */
    int insert(Order order);

    /**
     * 根据ID更新订单
     */
    int updateById(Order order);

    /**
     * 根据ID删除订单
     */
    int deleteById(Long id);

    /**
     * 根据ID查询订单
     */
    Order selectById(Long id);

    /**
     * 根据订单编号查询订单
     */
    Order selectByOrderSn(String orderSn);

    /**
     * 根据会员ID查询订单列表
     */
    List<Order> selectByMemberId(Long memberId);

    /**
     * 根据会员ID和状态查询订单列表
     */
    List<Order> selectByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") Integer status);

    /**
     * 统计会员订单数量
     */
    Long countByMemberId(Long memberId);

    /**
     * 统计会员各状态订单数量
     */
    Long countByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") Integer status);

    /**
     * 根据状态查询订单列表
     */
    List<Order> selectByStatus(Integer status);
}