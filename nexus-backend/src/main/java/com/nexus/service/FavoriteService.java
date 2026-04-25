package com.nexus.service;

import com.nexus.model.dto.FavoriteDTO;
import com.nexus.model.vo.FavoriteVO;
import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /**
     * 获取当前用户所有收藏
     */
    List<FavoriteVO> getCurrentUserFavorites();

    /**
     * 分页获取当前用户收藏
     */
    List<FavoriteVO> getCurrentUserFavoritesPage(Integer pageNum, Integer pageSize);

    /**
     * 添加收藏
     */
    Long addFavorite(FavoriteDTO favoriteDTO);

    /**
     * 删除收藏（按收藏ID）
     */
    void removeFavorite(Long favoriteId);

    /**
     * 删除收藏（按商品ID）
     */
    void removeFavoriteByProductId(Long productId);

    /**
     * 检查是否已收藏某商品
     */
    boolean isFavorite(Long productId);

    /**
     * 获取收藏数量
     */
    Long getFavoriteCount();

    /**
     * 从购物车移入收藏
     */
    Long moveFromCart(Long productId, Long cartItemId);
}