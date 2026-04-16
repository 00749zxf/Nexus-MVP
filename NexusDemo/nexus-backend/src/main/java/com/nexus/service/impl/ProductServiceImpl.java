package com.nexus.service.impl;

import com.nexus.mapper.ProductMapper;
import com.nexus.model.entity.Product;
import com.nexus.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    public List<Product> getAllProducts() {
        return productMapper.selectAll();
    }

    @Override
    public List<Product> getProductsByPage(int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.selectPage(offset, size);
    }

    @Override
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public List<Product> getProductsByCategory(Long categoryId) {
        return productMapper.selectByCategoryId(categoryId);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        List<Product> allProducts = productMapper.selectAll();
        return allProducts.stream()
                .filter(p -> p.getName().contains(keyword) ||
                        (p.getDescription() != null && p.getDescription().contains(keyword)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        productMapper.insert(product);
        return product;
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = productMapper.selectById(id);
        if (existingProduct == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setId(id);
        product.setUpdateTime(new Date());
        productMapper.updateById(product);
        return productMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }
}