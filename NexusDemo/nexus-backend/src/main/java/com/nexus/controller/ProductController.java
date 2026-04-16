package com.nexus.controller;

import com.nexus.common.Result;
import com.nexus.model.entity.Product;
import com.nexus.model.vo.ProductVO;
import com.nexus.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 获取商品列表
     */
    @GetMapping
    public Result<List<ProductVO>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {

        List<Product> products;

        if (keyword != null && !keyword.isEmpty()) {
            products = productService.searchProducts(keyword);
        } else if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId);
        } else {
            products = productService.getProductsByPage(page, size);
        }

        List<ProductVO> productVOs = products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(productVOs);
    }

    /**
     * 获取推荐商品
     */
    @GetMapping("/featured")
    public Result<List<ProductVO>> getFeaturedProducts() {
        List<Product> products = productService.getAllProducts();
        // 取前6个作为推荐商品
        List<ProductVO> productVOs = products.stream()
                .limit(6)
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(productVOs);
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(convertToVO(product));
    }

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    public Result<List<ProductVO>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        List<ProductVO> productVOs = products.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(productVOs);
    }

    /**
     * 创建商品（管理员）
     */
    @PostMapping
    public Result<ProductVO> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return Result.success(convertToVO(created));
    }

    /**
     * 更新商品（管理员）
     */
    @PutMapping("/{id}")
    public Result<ProductVO> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Product updated = productService.updateProduct(id, product);
        return Result.success(convertToVO(updated));
    }

    /**
     * 删除商品（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    /**
     * 转换为VO对象
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getPrice() != null ?
                product.getPrice().multiply(java.math.BigDecimal.valueOf(1.2)) : null);
        vo.setImage("https://via.placeholder.com/300x300/409EFF/fff?text=" +
                (product.getName() != null ? product.getName().substring(0, Math.min(10, product.getName().length())) : "Product"));
        vo.setStock(product.getStock());
        vo.setSales(100); // 模拟销量
        vo.setCategory("电子产品"); // 默认分类
        vo.setRating(4.5); // 模拟评分
        vo.setReviews(50); // 模拟评价数
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }
}