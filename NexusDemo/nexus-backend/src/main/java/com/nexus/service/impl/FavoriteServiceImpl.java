package com.nexus.service.impl;

import com.nexus.common.BusinessException;
import com.nexus.mapper.CartMapper;
import com.nexus.mapper.FavoriteMapper;
import com.nexus.mapper.MemberMapper;
import com.nexus.mapper.ProductMapper;
import com.nexus.model.entity.Cart;
import com.nexus.model.entity.Favorite;
import com.nexus.model.entity.Member;
import com.nexus.model.entity.Product;
import com.nexus.model.dto.FavoriteDTO;
import com.nexus.model.vo.FavoriteVO;
import com.nexus.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final MemberMapper memberMapper;
    private final ProductMapper productMapper;
    private final CartMapper cartMapper;

    @Override
    public List<FavoriteVO> getCurrentUserFavorites() {
        Long memberId = getCurrentMemberId();
        List<Favorite> favorites = favoriteMapper.selectByMemberId(memberId);

        return favorites.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FavoriteVO> getCurrentUserFavoritesPage(Integer pageNum, Integer pageSize) {
        Long memberId = getCurrentMemberId();
        Integer offset = (pageNum - 1) * pageSize;
        List<Favorite> favorites = favoriteMapper.selectByMemberIdPage(memberId, offset, pageSize);

        return favorites.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long addFavorite(FavoriteDTO favoriteDTO) {
        Long memberId = getCurrentMemberId();
        Long productId = favoriteDTO.getProductId();

        // 检查商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查是否已收藏
        if (favoriteMapper.existsByMemberIdAndProductId(memberId, productId)) {
            throw new BusinessException("已收藏该商品");
        }

        // 添加收藏
        Favorite favorite = new Favorite();
        favorite.setMemberId(memberId);
        favorite.setProductId(productId);
        favorite.setCreateTime(new Date());
        favorite.setUpdateTime(new Date());

        favoriteMapper.insert(favorite);
        log.info("添加收藏: memberId={}, productId={}", memberId, productId);

        return favorite.getId();
    }

    @Override
    @Transactional
    public void removeFavorite(Long favoriteId) {
        Long memberId = getCurrentMemberId();

        Favorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null) {
            throw new BusinessException("收藏不存在");
        }

        // 检查是否属于当前用户
        if (!favorite.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此收藏");
        }

        favoriteMapper.deleteById(favoriteId);
        log.info("删除收藏: favoriteId={}", favoriteId);
    }

    @Override
    @Transactional
    public void removeFavoriteByProductId(Long productId) {
        Long memberId = getCurrentMemberId();

        favoriteMapper.deleteByMemberIdAndProductId(memberId, productId);
        log.info("删除收藏(按商品): memberId={}, productId={}", memberId, productId);
    }

    @Override
    public boolean isFavorite(Long productId) {
        Long memberId = getCurrentMemberId();
        return favoriteMapper.existsByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public Long getFavoriteCount() {
        Long memberId = getCurrentMemberId();
        return favoriteMapper.countByMemberId(memberId);
    }

    @Override
    @Transactional
    public Long moveFromCart(Long productId, Long cartItemId) {
        Long memberId = getCurrentMemberId();

        // 检查购物车项是否存在并属于当前用户
        Cart cart = cartMapper.selectById(cartItemId);
        if (cart == null || !cart.getMemberId().equals(memberId)) {
            throw new BusinessException("购物车项不存在或无权操作");
        }

        // 添加收藏
        FavoriteDTO favoriteDTO = new FavoriteDTO();
        favoriteDTO.setProductId(productId);
        Long favoriteId = addFavorite(favoriteDTO);

        // 删除购物车项
        cartMapper.deleteById(cartItemId);
        log.info("从购物车移入收藏: memberId={}, productId={}, cartItemId={}", memberId, productId, cartItemId);

        return favoriteId;
    }

    /**
     * 从SecurityContext获取当前认证的用户ID
     */
    private Long getCurrentMemberId() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new BusinessException("用户未登录");
        }

        Member member = memberMapper.selectByUsername(username);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        return member.getId();
    }

    /**
     * 从SecurityContext获取当前认证的用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 将Favorite实体转换为FavoriteVO
     */
    private FavoriteVO convertToVO(Favorite favorite) {
        FavoriteVO vo = new FavoriteVO();
        BeanUtils.copyProperties(favorite, vo);

        // 获取商品信息
        Product product = productMapper.selectById(favorite.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductPrice(product.getPrice());
            vo.setProductImage("/images/product" + product.getId() + ".jpg");
            vo.setProductStock(product.getStock());
            vo.setProductAvailable(product.getStock() > 0);
        } else {
            vo.setProductName("商品已下架");
            vo.setProductPrice(java.math.BigDecimal.ZERO);
            vo.setProductStock(0);
            vo.setProductAvailable(false);
        }

        return vo;
    }
}