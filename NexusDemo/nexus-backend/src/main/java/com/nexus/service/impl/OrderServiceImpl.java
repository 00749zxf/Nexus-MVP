package com.nexus.service.impl;

import com.nexus.common.BusinessException;
import com.nexus.mapper.*;
import com.nexus.model.dto.OrderDTO;
import com.nexus.model.entity.*;
import com.nexus.model.vo.OrderItemVO;
import com.nexus.model.vo.OrderVO;
import com.nexus.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final MemberMapper memberMapper;

    private static final int STATUS_PENDING = 0;    // 待支付
    private static final int STATUS_PAID = 1;       // 已支付
    private static final int STATUS_SHIPPED = 2;    // 已发货
    private static final int STATUS_COMPLETED = 3;  // 已完成
    private static final int STATUS_CANCELLED = 4;  // 已取消

    @Override
    @Transactional
    public OrderVO createOrderFromCart(OrderDTO orderDTO) {
        Long memberId = getCurrentMemberId();

        // 获取购物车选中商品
        List<Cart> cartItems = cartMapper.selectByMemberId(memberId);
        List<Cart> selectedItems = cartItems.stream()
                .filter(Cart::getSelected)
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }

        // 检查库存并计算总价
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Cart cart : selectedItems) {
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在: " + cart.getProductId());
            }
            if (product.getStock() < cart.getQuantity()) {
                throw new BusinessException("商品库存不足: " + product.getName());
            }

            // 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        // 创建订单
        Order order = new Order();
        order.setOrderSn(generateOrderSn());
        order.setMemberId(memberId);
        order.setTotalAmount(totalAmount);
        order.setStatus(STATUS_PENDING);
        order.setReceiverName(orderDTO.getReceiverName());
        order.setReceiverPhone(orderDTO.getReceiverPhone());
        order.setReceiverAddress(orderDTO.getReceiverAddress());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        orderMapper.insert(order);

        // 插入订单项
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItem.setCreateTime(new Date());
        }
        orderItemMapper.batchInsert(orderItems);

        // 减库存
        for (Cart cart : selectedItems) {
            Product product = productMapper.selectById(cart.getProductId());
            product.setStock(product.getStock() - cart.getQuantity());
            productMapper.updateById(product);
        }

        // 清空已购买的购物车项
        for (Cart cart : selectedItems) {
            cartMapper.deleteById(cart.getId());
        }

        log.info("创建订单成功: orderSn={}, memberId={}, totalAmount={}",
                order.getOrderSn(), memberId, totalAmount);

        return convertToVO(order, orderItems);
    }

    @Override
    @Transactional
    public OrderVO createOrderDirect(OrderDTO orderDTO) {
        Long memberId = getCurrentMemberId();

        // 检查商品
        Product product = productMapper.selectById(orderDTO.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getStock() < orderDTO.getQuantity()) {
            throw new BusinessException("商品库存不足");
        }

        // 创建订单项
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setProductPrice(product.getPrice());
        orderItem.setQuantity(orderDTO.getQuantity());
        orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(orderDTO.getQuantity())));

        BigDecimal totalAmount = orderItem.getTotalPrice();

        // 创建订单
        Order order = new Order();
        order.setOrderSn(generateOrderSn());
        order.setMemberId(memberId);
        order.setTotalAmount(totalAmount);
        order.setStatus(STATUS_PENDING);
        order.setReceiverName(orderDTO.getReceiverName());
        order.setReceiverPhone(orderDTO.getReceiverPhone());
        order.setReceiverAddress(orderDTO.getReceiverAddress());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        orderMapper.insert(order);

        // 插入订单项
        orderItem.setOrderId(order.getId());
        orderItem.setCreateTime(new Date());
        orderItemMapper.insert(orderItem);

        // 减库存
        product.setStock(product.getStock() - orderDTO.getQuantity());
        productMapper.updateById(product);

        log.info("直接购买创建订单成功: orderSn={}, memberId={}, productId={}",
                order.getOrderSn(), memberId, orderDTO.getProductId());

        return convertToVO(order, List.of(orderItem));
    }

    @Override
    public List<OrderVO> getCurrentUserOrders() {
        Long memberId = getCurrentMemberId();
        List<Order> orders = orderMapper.selectByMemberId(memberId);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
                    return convertToVO(order, orderItems);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getCurrentUserOrdersByStatus(Integer status) {
        Long memberId = getCurrentMemberId();
        List<Order> orders = orderMapper.selectByMemberIdAndStatus(memberId, status);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
                    return convertToVO(order, orderItems);
                })
                .collect(Collectors.toList());
    }

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Long memberId = getCurrentMemberId();
        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 检查是否属于当前用户
        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException("无权查看此订单");
        }

        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        return convertToVO(order, orderItems);
    }

    @Override
    @Transactional
    public OrderVO payOrder(Long orderId) {
        Long memberId = getCurrentMemberId();
        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException("订单状态不正确，无法支付");
        }

        order.setStatus(STATUS_PAID);
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);

        log.info("订单支付成功: orderId={}, orderSn={}", orderId, order.getOrderSn());

        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        return convertToVO(order, orderItems);
    }

    @Override
    @Transactional
    public OrderVO cancelOrder(Long orderId) {
        Long memberId = getCurrentMemberId();
        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException("订单状态不正确，无法取消");
        }

        // 恢复库存
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }

        order.setStatus(STATUS_CANCELLED);
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);

        log.info("订单取消成功: orderId={}, orderSn={}", orderId, order.getOrderSn());

        return convertToVO(order, orderItems);
    }

    @Override
    @Transactional
    public OrderVO confirmOrder(Long orderId) {
        Long memberId = getCurrentMemberId();
        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != STATUS_SHIPPED) {
            throw new BusinessException("订单状态不正确，无法确认收货");
        }

        order.setStatus(STATUS_COMPLETED);
        order.setUpdateTime(new Date());
        orderMapper.updateById(order);

        log.info("订单确认收货成功: orderId={}, orderSn={}", orderId, order.getOrderSn());

        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        return convertToVO(order, orderItems);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Long memberId = getCurrentMemberId();
        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 只有已完成或已取消的订单才能删除
        if (order.getStatus() != STATUS_COMPLETED && order.getStatus() != STATUS_CANCELLED) {
            throw new BusinessException("订单状态不正确，无法删除");
        }

        // 删除订单项
        orderItemMapper.deleteByOrderId(orderId);
        // 删除订单
        orderMapper.deleteById(orderId);

        log.info("订单删除成功: orderId={}", orderId);
    }

    @Override
    public Long getOrderCountByStatus(Integer status) {
        Long memberId = getCurrentMemberId();
        return orderMapper.countByMemberIdAndStatus(memberId, status);
    }

    /**
     * 生成订单编号
     */
    private String generateOrderSn() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD" + timestamp + random;
    }

    /**
     * 从SecurityContext获取当前认证的用户ID
     */
    private Long getCurrentMemberId() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new BusinessException("用户未登录");
        }

        Member member = memberMapper.selectByUsername(username);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        return member.getId();
    }

    /**
     * 从SecurityContext获取当前认证的用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 将Order实体转换为OrderVO
     */
    private OrderVO convertToVO(Order order, List<OrderItem> orderItems) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 设置状态描述
        vo.setStatusDesc(getStatusDesc(order.getStatus()));

        // 转换订单项
        List<OrderItemVO> itemVOs = orderItems.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
        vo.setOrderItems(itemVOs);

        // 计算商品总数
        int totalQuantity = orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        vo.setTotalQuantity(totalQuantity);

        return vo;
    }

    /**
     * 将OrderItem实体转换为OrderItemVO
     */
    private OrderItemVO convertItemToVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);

        // 设置商品图片（模拟）
        vo.setProductImage("https://via.placeholder.com/100x100/409EFF/fff?text=" +
                (item.getProductName() != null ? item.getProductName().substring(0, Math.min(5, item.getProductName().length())) : "P"));

        return vo;
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case STATUS_PENDING: return "待支付";
            case STATUS_PAID: return "已支付";
            case STATUS_SHIPPED: return "已发货";
            case STATUS_COMPLETED: return "已完成";
            case STATUS_CANCELLED: return "已取消";
            default: return "未知";
        }
    }
}