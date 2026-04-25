package com.nexus.agent.tools;

import com.nexus.mapper.ProductMapper;
import com.nexus.model.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 商品查询工具
 * 提供商品信息查询能力给Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductQueryTool implements Tool {

    private final ProductMapper productMapper;

    @Override
    public String getName() {
        return "queryProduct";
    }

    @Override
    public String getDescription() {
        return "查询商品信息，支持按ID、名称、分类查询。返回商品名称、价格、库存、状态等信息。";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Long productId = getLongParam(params, "productId");
        String name = getStringParam(params, "name");
        Long categoryId = getLongParam(params, "categoryId");
        Integer status = getIntegerParam(params, "status");

        try {
            // 按ID查询
            if (productId != null) {
                Product product = productMapper.selectById(productId);
                if (product == null) {
                    return ToolResult.error("商品不存在: " + productId);
                }
                return ToolResult.ok(toProductInfo(product));
            }

            // 按名称搜索
            if (name != null) {
                List<Product> products = productMapper.selectByNameLike(name);
                return ToolResult.ok(products.stream().map(this::toProductInfo).toList(),
                        "找到 " + products.size() + " 个商品");
            }

            // 按分类查询
            if (categoryId != null) {
                List<Product> products = productMapper.selectByCategoryId(categoryId);
                return ToolResult.ok(products.stream().map(this::toProductInfo).toList(),
                        "分类下有 " + products.size() + " 个商品");
            }

            // 查询上架商品列表
            if (status != null) {
                List<Product> products = productMapper.selectByStatus(status);
                return ToolResult.ok(products.stream().map(this::toProductInfo).toList(),
                        "状态 " + status + " 下有 " + products.size() + " 个商品");
            }

            // 默认查询热门商品（上架的前10个）
            List<Product> products = productMapper.selectByStatus(1);
            return ToolResult.ok(products.stream()
                    .limit(10)
                    .map(this::toProductInfo)
                    .toList(), "热门商品列表");

        } catch (Exception e) {
            log.error("商品查询失败", e);
            return ToolResult.error("商品查询失败: " + e.getMessage());
        }
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setParams(List.of(
                ToolSchema.ParamDef.of("productId", "number", "商品ID，精确查询", false),
                ToolSchema.ParamDef.of("name", "string", "商品名称关键词，模糊搜索", false),
                ToolSchema.ParamDef.of("categoryId", "number", "分类ID", false),
                ToolSchema.ParamDef.of("status", "number", "商品状态：0-下架，1-上架", false)
        ));
        return schema;
    }

    private ProductInfo toProductInfo(Product product) {
        ProductInfo info = new ProductInfo();
        info.setId(product.getId());
        info.setName(product.getName());
        info.setDescription(product.getDescription());
        info.setPrice(product.getPrice());
        info.setStock(product.getStock());
        info.setStatus(product.getStatus());
        info.setStatusDesc(product.getStatus() == 1 ? "上架" : "下架");
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
    public static class ProductInfo {
        private Long id;
        private String name;
        private String description;
        private java.math.BigDecimal price;
        private Integer stock;
        private Integer status;
        private String statusDesc;
    }
}