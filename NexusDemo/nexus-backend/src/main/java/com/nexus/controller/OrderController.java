package com.nexus.controller;

import com.nexus.common.Result;
import com.nexus.model.dto.OrderDTO;
import com.nexus.model.vo.OrderVO;
import com.nexus.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 订单控制器
 */
@Tag(name = "订单管理", description = "订单创建、支付、取消、查询")
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（从购物车）")
    @PostMapping("/from-cart")
    public Result<OrderVO> createOrderFromCart(@Valid @RequestBody OrderDTO orderDTO) {
        OrderVO order = orderService.createOrderFromCart(orderDTO);
        return Result.success("订单创建成功", order);
    }

    @Operation(summary = "创建订单（直接购买）")
    @PostMapping("/direct")
    public Result<OrderVO> createOrderDirect(@Valid @RequestBody OrderDTO orderDTO) {
        OrderVO order = orderService.createOrderDirect(orderDTO);
        return Result.success("订单创建成功", order);
    }

    @Operation(summary = "获取当前用户订单列表")
    @GetMapping
    public Result<List<OrderVO>> getOrders(@RequestParam(required = false) Integer status) {
        List<OrderVO> orders;
        if (status != null) {
            orders = orderService.getCurrentUserOrdersByStatus(status);
        } else {
            orders = orderService.getCurrentUserOrders();
        }
        return Result.success(orders);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long orderId) {
        OrderVO order = orderService.getOrderDetail(orderId);
        return Result.success(order);
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{orderId}/pay")
    public Result<OrderVO> payOrder(@PathVariable Long orderId) {
        OrderVO order = orderService.payOrder(orderId);
        return Result.success("支付成功", order);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderId}/cancel")
    public Result<OrderVO> cancelOrder(@PathVariable Long orderId) {
        OrderVO order = orderService.cancelOrder(orderId);
        return Result.success("订单已取消", order);
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderId}/confirm")
    public Result<OrderVO> confirmOrder(@PathVariable Long orderId) {
        OrderVO order = orderService.confirmOrder(orderId);
        return Result.success("已确认收货", order);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{orderId}")
    public Result<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return Result.success("订单已删除", null);
    }

    @Operation(summary = "获取订单状态统计")
    @GetMapping("/count")
    public Result<Long> getOrderCount(@RequestParam(required = false) Integer status) {
        Long count = orderService.getOrderCountByStatus(status);
        return Result.success(count);
    }
}