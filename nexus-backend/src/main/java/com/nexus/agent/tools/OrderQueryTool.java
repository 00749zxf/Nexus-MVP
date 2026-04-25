package com.nexus.agent.tools;

import com.nexus.mapper.OrderMapper;
import com.nexus.mapper.OrderItemMapper;
import com.nexus.model.entity.Order;
import com.nexus.model.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 订单查询工具
 * 提供订单信息查询能力给Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderQueryTool implements Tool {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public String getName() {
        return "queryOrder";
    }

    @Override
    public String getDescription() {
        return "查询订单信息，支持按订单ID、订单编号、用户ID、状态查询。返回订单详情、商品列表、状态等信息。";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Long orderId = getLongParam(params, "orderId");
        String orderSn = getStringParam(params, "orderSn");
        Long memberId = getLongParam(params, "memberId");
        Integer status = getIntegerParam(params, "status");

        try {
            // 按ID查询
            if (orderId != null) {
                Order order = orderMapper.selectById(orderId);
                if (order == null) {
                    return ToolResult.error("订单不存在: " + orderId);
                }
                List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
                return ToolResult.ok(toOrderInfo(order, items));
            }

            // 按订单编号查询
            if (orderSn != null) {
                Order order = orderMapper.selectByOrderSn(orderSn);
                if (order == null) {
                    return ToolResult.error("订单不存在: " + orderSn);
                }
                List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
                return ToolResult.ok(toOrderInfo(order, items));
            }

            // 按用户查询
            if (memberId != null) {
                List<Order> orders = orderMapper.selectByMemberId(memberId);
                return ToolResult.ok(orders.stream()
                        .map(o -> {
                            List<OrderItem> items = orderItemMapper.selectByOrderId(o.getId());
                            return toOrderInfo(o, items);
                        })
                        .toList(), "用户有 " + orders.size() + " 个订单");
            }

            // 按状态查询
            if (status != null) {
                List<Order> orders = orderMapper.selectByStatus(status);
                return ToolResult.ok(orders.stream()
                        .map(o -> {
                            List<OrderItem> items = orderItemMapper.selectByOrderId(o.getId());
                            return toOrderInfo(o, items);
                        })
                        .toList(), "状态 " + getStatusDesc(status) + " 下有 " + orders.size() + " 个订单");
            }

            return ToolResult.error("请提供查询条件：orderId、orderSn、memberId 或 status");

        } catch (Exception e) {
            log.error("订单查询失败", e);
            return ToolResult.error("订单查询失败: " + e.getMessage());
        }
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setParams(List.of(
                ToolSchema.ParamDef.of("orderId", "number", "订单ID", false),
                ToolSchema.ParamDef.of("orderSn", "string", "订单编号", false),
                ToolSchema.ParamDef.of("memberId", "number", "用户ID", false),
                ToolSchema.ParamDef.of("status", "number", "订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消", false)
        ));
        return schema;
    }

    private OrderInfo toOrderInfo(Order order, List<OrderItem> items) {
        OrderInfo info = new OrderInfo();
        info.setId(order.getId());
        info.setOrderSn(order.getOrderSn());
        info.setMemberId(order.getMemberId());
        info.setTotalAmount(order.getTotalAmount());
        info.setStatus(order.getStatus());
        info.setStatusDesc(getStatusDesc(order.getStatus()));
        info.setReceiverName(order.getReceiverName());
        info.setReceiverPhone(order.getReceiverPhone());
        info.setReceiverAddress(order.getReceiverAddress());
        info.setCreateTime(order.getCreateTime());
        info.setItems(items.stream().map(this::toOrderItemInfo).toList());
        return info;
    }

    private OrderItemInfo toOrderItemInfo(OrderItem item) {
        OrderItemInfo info = new OrderItemInfo();
        info.setProductId(item.getProductId());
        info.setProductName(item.getProductName());
        info.setProductPrice(item.getProductPrice());
        info.setQuantity(item.getQuantity());
        info.setTotalPrice(item.getTotalPrice());
        return info;
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }

    private String getStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntegerParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        return null;
    }

    @lombok.Data
    public static class OrderInfo {
        private Long id;
        private String orderSn;
        private Long memberId;
        private java.math.BigDecimal totalAmount;
        private Integer status;
        private String statusDesc;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private java.util.Date createTime;
        private List<OrderItemInfo> items;
    }

    @lombok.Data
    public static class OrderItemInfo {
        private Long productId;
        private String productName;
        private java.math.BigDecimal productPrice;
        private Integer quantity;
        private java.math.BigDecimal totalPrice;
    }
}