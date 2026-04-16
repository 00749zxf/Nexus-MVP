package com.nexus.mapper;

import com.nexus.model.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单项Mapper接口
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 插入订单项
     */
    int insert(OrderItem orderItem);

    /**
     * 批量插入订单项
     */
    int batchInsert(@Param("list") List<OrderItem> orderItems);

    /**
     * 根据ID删除订单项
     */
    int deleteById(Long id);

    /**
     * 根据订单ID删除订单项
     */
    int deleteByOrderId(Long orderId);

    /**
     * 根据ID查询订单项
     */
    OrderItem selectById(Long id);

    /**
     * 根据订单ID查询订单项列表
     */
    List<OrderItem> selectByOrderId(Long orderId);
}