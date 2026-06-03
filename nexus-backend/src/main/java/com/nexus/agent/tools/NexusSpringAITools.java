package com.nexus.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring AI Tool 适配层
 * 将现有 Nexus Tool 接口实现包装为 Spring AI @Tool 注解方法
 * LLM 通过 Spring AI 的 Function Calling 机制自动调用这些方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NexusSpringAITools {

    private final ProductQueryTool productQueryTool;
    private final OrderQueryTool orderQueryTool;
    private final UserQueryTool userQueryTool;
    private final CartQueryTool cartQueryTool;
    private final ObjectMapper objectMapper;

    @Tool(description = "查询商品信息。支持：productId精确查询，name模糊搜索，categoryId分类查询，status状态过滤(1=上架,0=下架)。不传参数时返回热门商品列表。")
    public String queryProduct(Long productId, String name, Long categoryId, Integer status) {
        Map<String, Object> params = new HashMap<>();
        if (productId != null) params.put("productId", productId);
        if (name != null) params.put("name", name);
        if (categoryId != null) params.put("categoryId", categoryId);
        if (status != null) params.put("status", status);
        return toJson(productQueryTool.execute(params));
    }

    @Tool(description = "查询订单信息。支持：orderId订单ID查询，orderSn订单编号查询，memberId查询用户所有订单，status状态过滤(0=待支付,1=已支付,2=已发货,3=已完成,4=已取消)。")
    public String queryOrder(Long orderId, String orderSn, Long memberId, Integer status) {
        Map<String, Object> params = new HashMap<>();
        if (orderId != null) params.put("orderId", orderId);
        if (orderSn != null) params.put("orderSn", orderSn);
        if (memberId != null) params.put("memberId", memberId);
        if (status != null) params.put("status", status);
        return toJson(orderQueryTool.execute(params));
    }

    @Tool(description = "查询用户信息。需要提供 memberId（用户ID）或 username（用户名）其中之一。")
    public String queryUser(Long memberId, String username) {
        Map<String, Object> params = new HashMap<>();
        if (memberId != null) params.put("memberId", memberId);
        if (username != null) params.put("username", username);
        return toJson(userQueryTool.execute(params));
    }

    @Tool(description = "查询用户购物车内容。需要提供 memberId（用户ID）。")
    public String queryCart(Long memberId) {
        Map<String, Object> params = new HashMap<>();
        if (memberId != null) params.put("memberId", memberId);
        return toJson(cartQueryTool.execute(params));
    }

    private String toJson(ToolResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("序列化工具结果失败，使用 toString", e);
            return result.toString();
        }
    }
}