package com.nexus.service;

import com.nexus.model.entity.Product;
import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 获取所有商品
     */
    List<Product> getAllProducts();

    /**
     * 分页获取商品
     */
    List<Product> getProductsByPage(int page, int size);

    /**
     * 根据ID获取商品
     */
    Product getProductById(Long id);

    /**
     * 根据分类获取商品
     */
    List<Product> getProductsByCategory(Long categoryId);

    /**
     * 搜索商品
     */
    List<Product> searchProducts(String keyword);

    /**
     * 创建商品
     */
    Product createProduct(Product product);

    /**
     * 更新商品
     */
    Product updateProduct(Long id, Product product);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);
}