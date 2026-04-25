package com.nexus.service;

import com.nexus.model.dto.OrderDTO;
import com.nexus.model.vo.OrderVO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单（从购物车）
     */
    OrderVO createOrderFromCart(OrderDTO orderDTO);

    /**
     * 创建订单（直接购买）
     */
    OrderVO createOrderDirect(OrderDTO orderDTO);

    /**
     * 获取当前用户订单列表
     */
    List<OrderVO> getCurrentUserOrders();

    /**
     * 根据状态获取当前用户订单列表
     */
    List<OrderVO> getCurrentUserOrdersByStatus(Integer status);

    /**
     * 获取订单详情
     */
    OrderVO getOrderDetail(Long orderId);

    /**
     * 支付订单
     */
    OrderVO payOrder(Long orderId);

    /**
     * 取消订单
     */
    OrderVO cancelOrder(Long orderId);

    /**
     * 确认收货
     */
    OrderVO confirmOrder(Long orderId);

    /**
     * 删除订单
     */
    void deleteOrder(Long orderId);

    /**
     * 获取订单状态统计
     */
    Long getOrderCountByStatus(Integer status);
}