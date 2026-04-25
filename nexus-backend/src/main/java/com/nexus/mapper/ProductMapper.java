package com.nexus.mapper;

import com.nexus.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品Mapper接口
 */
@Mapper
public interface ProductMapper {

    /**
     * 插入商品
     */
    int insert(Product product);

    /**
     * 根据ID更新商品
     */
    int updateById(Product product);

    /**
     * 根据ID删除商品
     */
    int deleteById(Long id);

    /**
     * 根据ID查询商品
     */
    Product selectById(Long id);

    /**
     * 查询所有商品
     */
    List<Product> selectAll();

    /**
     * 分页查询商品
     */
    List<Product> selectPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计商品数量
     */
    Long count();

    /**
     * 根据分类ID查询商品
     */
    List<Product> selectByCategoryId(Long categoryId);

    /**
     * 根据状态查询商品
     */
    List<Product> selectByStatus(Integer status);

    /**
     * 根据名称模糊查询商品
     */
    List<Product> selectByNameLike(String name);
}