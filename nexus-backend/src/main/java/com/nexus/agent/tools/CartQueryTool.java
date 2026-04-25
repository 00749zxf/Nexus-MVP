package com.nexus.agent.tools;

import com.nexus.mapper.CartMapper;
import com.nexus.model.entity.Cart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 购物车查询工具
 * 提供购物车信息查询能力给Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartQueryTool implements Tool {

    private final CartMapper cartMapper;

    @Override
    public String getName() {
        return "queryCart";
    }

    @Override
    public String getDescription() {
        return "查询购物车信息，支持按用户ID查询。返回购物车商品列表、数量、选中状态。";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Long memberId = getLongParam(params, "memberId");
        Long cartId = getLongParam(params, "cartId");

        try {
            if (cartId != null) {
                Cart cart = cartMapper.selectById(cartId);
                if (cart == null) {
                    return ToolResult.error("购物车项不存在: " + cartId);
                }
                return ToolResult.ok(toCartInfo(cart));
            }

            if (memberId != null) {
                List<Cart> carts = cartMapper.selectByMemberId(memberId);
                if (carts.isEmpty()) {
                    return ToolResult.ok(List.of(), "购物车为空");
                }
                return ToolResult.ok(carts.stream().map(this::toCartInfo).toList(),
                        "购物车有 " + carts.size() + " 个商品");
            }

            return ToolResult.error("请提供查询条件：memberId 或 cartId");

        } catch (Exception e) {
            log.error("购物车查询失败", e);
            return ToolResult.error("购物车查询失败: " + e.getMessage());
        }
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setParams(List.of(
                ToolSchema.ParamDef.of("memberId", "number", "用户ID，查询用户购物车", false),
                ToolSchema.ParamDef.of("cartId", "number", "购物车项ID", false)
        ));
        return schema;
    }

    private CartInfo toCartInfo(Cart cart) {
        CartInfo info = new CartInfo();
        info.setId(cart.getId());
        info.setMemberId(cart.getMemberId());
        info.setProductId(cart.getProductId());
        info.setQuantity(cart.getQuantity());
        info.setSelected(cart.getSelected());
        return info;
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }

    @lombok.Data
    public static class CartInfo {
        private Long id;
        private Long memberId;
        private Long productId;
        private Integer quantity;
        private Boolean selected;
    }
}