package com.nexus.mapper;

import com.nexus.model.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 收藏Mapper接口
 */
@Mapper
public interface FavoriteMapper {

    /**
     * 插入收藏
     */
    int insert(Favorite favorite);

    /**
     * 根据ID删除收藏
     */
    int deleteById(Long id);

    /**
     * 根据会员ID和商品ID删除收藏
     */
    int deleteByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    /**
     * 根据ID查询收藏
     */
    Favorite selectById(Long id);

    /**
     * 根据会员ID和商品ID查询收藏
     */
    Favorite selectByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    /**
     * 根据会员ID查询所有收藏
     */
    List<Favorite> selectByMemberId(Long memberId);

    /**
     * 根据会员ID分页查询收藏
     */
    List<Favorite> selectByMemberIdPage(@Param("memberId") Long memberId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计会员收藏数量
     */
    Long countByMemberId(Long memberId);

    /**
     * 检查会员是否收藏了某商品
     */
    boolean existsByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);
}