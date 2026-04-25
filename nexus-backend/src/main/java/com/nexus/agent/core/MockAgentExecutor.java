package com.nexus.agent.core;

import com.nexus.agent.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模拟Agent执行器
 * 阶段1使用，暂不接入真实LLM
 * 使用模板生成响应
 */
@Slf4j
@Component
public class MockAgentExecutor implements AgentExecutor {

    @Override
    public AgentResponse executeWithLLM(AgentRequest request) {
        // Mock模式不调用LLM，直接返回空响应
        AgentResponse response = new AgentResponse();
        response.setSessionId("mock-session");
        response.setResponse("Mock模式不支持LLM调用。请启用DeepSeek模式。");
        response.setNeedHumanSupport(false);
        return response;
    }

    @Override
    public String generateResponse(AgentRequest request, Map<String, Object> toolResults) {
        String message = request.getMessage();
        AgentRequest.AgentType agentType = request.getAgentType();

        // 根据工具结果生成响应
        if (toolResults.containsKey("queryProduct")) {
            return generateProductResponse(toolResults.get("queryProduct"));
        }

        if (toolResults.containsKey("queryOrder")) {
            return generateOrderResponse(toolResults.get("queryOrder"), request);
        }

        if (toolResults.containsKey("queryCart")) {
            return generateCartResponse(toolResults.get("queryCart"));
        }

        if (toolResults.containsKey("queryUser")) {
            return generateUserResponse(toolResults.get("queryUser"));
        }

        // 默认响应
        return "您好，我是Nexus智能客服助手。请问有什么可以帮助您的？\n\n您可以咨询商品信息、查询订单状态、了解售后政策等问题。";
    }

    private String generateProductResponse(Object productData) {
        StringBuilder sb = new StringBuilder();

        if (productData instanceof java.util.List) {
            java.util.List<?> products = (java.util.List<?>) productData;
            if (products.isEmpty()) {
                sb.append("抱歉，没有找到相关商品。");
            } else {
                sb.append("为您找到以下商品：\n\n");
                for (int i = 0; i < Math.min(5, products.size()); i++) {
                    Object p = products.get(i);
                    if (p instanceof ProductQueryTool.ProductInfo) {
                        ProductQueryTool.ProductInfo product = (ProductQueryTool.ProductInfo) p;
                        sb.append(String.format("- **%s**（ID: %d）\n", product.getName(), product.getId()));
                        sb.append(String.format("  价格: ¥%.2f | 库存: %d件\n\n",
                                product.getPrice(), product.getStock()));
                    }
                }
                if (products.size() > 5) {
                    sb.append("还有更多商品，您可以在商品页面查看完整列表。");
                }
            }
        } else if (productData instanceof ProductQueryTool.ProductInfo) {
            ProductQueryTool.ProductInfo product = (ProductQueryTool.ProductInfo) productData;
            sb.append(String.format("商品详情：**%s**\n\n", product.getName()));
            sb.append(String.format("- 价格: ¥%.2f\n", product.getPrice()));
            sb.append(String.format("- 库存: %d件\n", product.getStock()));
            sb.append(String.format("- 状态: %s\n", product.getStatusDesc()));
            if (product.getDescription() != null) {
                sb.append(String.format("- 描述: %s\n", product.getDescription()));
            }
        }

        return sb.toString();
    }

    private String generateOrderResponse(Object orderData, AgentRequest request) {
        StringBuilder sb = new StringBuilder();
        String message = request.getMessage().toLowerCase();

        if (orderData instanceof java.util.List) {
            java.util.List<?> orders = (java.util.List<?>) orderData;
            if (orders.isEmpty()) {
                sb.append("您目前没有相关订单。");
            } else {
                sb.append("您的订单列表：\n\n");
                for (int i = 0; i < Math.min(5, orders.size()); i++) {
                    Object o = orders.get(i);
                    if (o instanceof OrderQueryTool.OrderInfo) {
                        OrderQueryTool.OrderInfo order = (OrderQueryTool.OrderInfo) o;
                        sb.append(String.format("- 订单号: **%s**\n", order.getOrderSn()));
                        sb.append(String.format("  金额: ¥%.2f | 状态: %s\n\n",
                                order.getTotalAmount(), order.getStatusDesc()));
                    }
                }
            }
        } else if (orderData instanceof OrderQueryTool.OrderInfo) {
            OrderQueryTool.OrderInfo order = (OrderQueryTool.OrderInfo) orderData;

            // 根据用户问题定制回复
            if (message.contains("发货") || message.contains("快递") || message.contains("物流")) {
                sb.append(String.format("您的订单 **%s** 目前状态为「%s」。\n\n",
                        order.getOrderSn(), order.getStatusDesc()));

                switch (order.getStatus()) {
                    case 0:
                        sb.append("订单尚未支付，请先完成支付后才会安排发货。");
                        break;
                    case 1:
                        sb.append("订单已支付，正在等待发货。通常会在支付后24小时内发货，请耐心等待。");
                        break;
                    case 2:
                        sb.append("订单已发货，正在配送中。收货地址：" + order.getReceiverAddress());
                        break;
                    case 3:
                        sb.append("订单已完成，您已收到商品。感谢您的购买！");
                        break;
                    case 4:
                        sb.append("订单已取消。如有疑问请联系客服。");
                        break;
                    default:
                        sb.append("订单状态正常，请耐心等待。");
                }
            } else if (message.contains("取消")) {
                if (order.getStatus() == 0) {
                    sb.append(String.format("订单 **%s** 可以取消。请前往订单详情页面点击取消按钮。\n",
                            order.getOrderSn()));
                } else {
                    sb.append(String.format("订单 **%s** 当前状态为「%s」，无法直接取消。\n",
                            order.getOrderSn(), order.getStatusDesc()));
                    sb.append("如需取消，请联系人工客服处理。");
                }
            } else {
                sb.append(String.format("订单详情：**%s**\n\n", order.getOrderSn()));
                sb.append(String.format("- 总金额: ¥%.2f\n", order.getTotalAmount()));
                sb.append(String.format("- 状态: %s\n", order.getStatusDesc()));
                sb.append(String.format("- 收货人: %s\n", order.getReceiverName()));
                sb.append(String.format("- 收货地址: %s\n", order.getReceiverAddress()));

                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    sb.append("\n商品清单：\n");
                    for (OrderQueryTool.OrderItemInfo item : order.getItems()) {
                        sb.append(String.format("- %s x%d = ¥%.2f\n",
                                item.getProductName(), item.getQuantity(), item.getTotalPrice()));
                    }
                }
            }
        }

        return sb.toString();
    }

    private String generateCartResponse(Object cartData) {
        StringBuilder sb = new StringBuilder();

        if (cartData instanceof java.util.List) {
            java.util.List<?> carts = (java.util.List<?>) cartData;
            if (carts.isEmpty()) {
                sb.append("您的购物车是空的。\n\n快去挑选心仪的商品吧！");
            } else {
                sb.append("您的购物车：\n\n");
                sb.append(String.format("共有 %d 个商品。\n\n", carts.size()));
                sb.append("您可以前往购物车页面查看详情并进行结算。");
            }
        } else if (cartData instanceof CartQueryTool.CartInfo) {
            CartQueryTool.CartInfo cart = (CartQueryTool.CartInfo) cartData;
            sb.append(String.format("购物车商品ID: %d，数量: %d件。\n",
                    cart.getProductId(), cart.getQuantity()));
        }

        return sb.toString();
    }

    private String generateUserResponse(Object userData) {
        StringBuilder sb = new StringBuilder();

        if (userData instanceof UserQueryTool.UserInfo) {
            UserQueryTool.UserInfo user = (UserQueryTool.UserInfo) userData;
            sb.append("您的账户信息：\n\n");
            sb.append(String.format("- 用户名: %s\n", user.getUsername()));
            sb.append(String.format("- 手机号: %s\n", user.getPhone()));
            sb.append(String.format("- 邮箱: %s\n", user.getEmail()));
            sb.append(String.format("- 状态: %s\n", user.getStatusDesc()));
        }

        return sb.toString();
    }
}