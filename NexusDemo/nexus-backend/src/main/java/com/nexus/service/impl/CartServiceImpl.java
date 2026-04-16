package com.nexus.service.impl;

import com.nexus.common.BusinessException;
import com.nexus.mapper.CartMapper;
import com.nexus.mapper.MemberMapper;
import com.nexus.mapper.ProductMapper;
import com.nexus.model.entity.Cart;
import com.nexus.model.entity.Member;
import com.nexus.model.entity.Product;
import com.nexus.model.dto.CartDTO;
import com.nexus.model.vo.CartVO;
import com.nexus.service.CartService;
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
 * 购物车服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final MemberMapper memberMapper;
    private final ProductMapper productMapper;

    @Override
    public List<CartVO> getCurrentUserCart() {
        Long memberId = getCurrentMemberId();
        List<Cart> cartItems = cartMapper.selectByMemberId(memberId);

        return cartItems.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long addToCart(CartDTO cartDTO) {
        Long memberId = getCurrentMemberId();
        Long productId = cartDTO.getProductId();

        // 检查商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查库存
        if (product.getStock() < cartDTO.getQuantity()) {
            throw new BusinessException("商品库存不足");
        }

        // 检查是否已存在购物车项
        Cart existingCart = cartMapper.selectByMemberIdAndProductId(memberId, productId);
        if (existingCart != null) {
            // 更新数量
            existingCart.setQuantity(existingCart.getQuantity() + cartDTO.getQuantity());
            // 检查库存
            if (product.getStock() < existingCart.getQuantity()) {
                throw new BusinessException("商品库存不足");
            }
            existingCart.setUpdateTime(new Date());
            cartMapper.updateById(existingCart);
            return existingCart.getId();
        } else {
            // 新增购物车项
            Cart cart = new Cart();
            cart.setMemberId(memberId);
            cart.setProductId(productId);
            cart.setQuantity(cartDTO.getQuantity());
            cart.setSelected(cartDTO.getSelected() != null ? cartDTO.getSelected() : true);
            cart.setCreateTime(new Date());
            cart.setUpdateTime(new Date());

            cartMapper.insert(cart);
            log.info("添加商品到购物车: memberId={}, productId={}, quantity={}",
                    memberId, productId, cartDTO.getQuantity());
            return cart.getId();
        }
    }

    @Override
    @Transactional
    public void updateQuantity(Long cartItemId, Integer quantity) {
        Long memberId = getCurrentMemberId();

        Cart cart = cartMapper.selectById(cartItemId);
        if (cart == null) {
            throw new BusinessException("购物车项不存在");
        }

        // 检查是否属于当前用户
        if (!cart.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此购物车项");
        }

        // 检查商品库存
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        if (product.getStock() < quantity) {
            throw new BusinessException("商品库存不足");
        }

        cart.setQuantity(quantity);
        cart.setUpdateTime(new Date());
        cartMapper.updateById(cart);

        log.info("更新购物车商品数量: cartItemId={}, quantity={}", cartItemId, quantity);
    }

    @Override
    @Transactional
    public void removeCartItem(Long cartItemId) {
        Long memberId = getCurrentMemberId();

        Cart cart = cartMapper.selectById(cartItemId);
        if (cart == null) {
            throw new BusinessException("购物车项不存在");
        }

        // 检查是否属于当前用户
        if (!cart.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此购物车项");
        }

        cartMapper.deleteById(cartItemId);
        log.info("删除购物车商品: cartItemId={}", cartItemId);
    }

    @Override
    @Transactional
    public void clearCart() {
        Long memberId = getCurrentMemberId();
        cartMapper.deleteByMemberId(memberId);
        log.info("清空购物车: memberId={}", memberId);
    }

    @Override
    @Transactional
    public void updateSelection(Long cartItemId, Boolean selected) {
        Long memberId = getCurrentMemberId();

        Cart cart = cartMapper.selectById(cartItemId);
        if (cart == null) {
            throw new BusinessException("购物车项不存在");
        }

        // 检查是否属于当前用户
        if (!cart.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此购物车项");
        }

        cart.setSelected(selected);
        cart.setUpdateTime(new Date());
        cartMapper.updateById(cart);

        log.info("更新购物车商品选择状态: cartItemId={}, selected={}", cartItemId, selected);
    }

    @Override
    @Transactional
    public void batchUpdateSelection(List<Long> cartItemIds, Boolean selected) {
        Long memberId = getCurrentMemberId();

        for (Long cartItemId : cartItemIds) {
            Cart cart = cartMapper.selectById(cartItemId);
            if (cart == null || !cart.getMemberId().equals(memberId)) {
                continue; // 跳过无权操作的项
            }

            cart.setSelected(selected);
            cart.setUpdateTime(new Date());
            cartMapper.updateById(cart);
        }

        log.info("批量更新购物车商品选择状态: cartItemIds={}, selected={}", cartItemIds, selected);
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
     * 将Cart实体转换为CartVO
     */
    private CartVO convertToVO(Cart cart) {
        CartVO vo = new CartVO();
        BeanUtils.copyProperties(cart, vo);

        // 获取商品信息
        Product product = productMapper.selectById(cart.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductPrice(product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
            // 注意：实际项目中商品图片可能有单独的字段，这里简单处理
            vo.setProductImage("/images/product" + product.getId() + ".jpg");
            vo.setStock(product.getStock());
            vo.setAvailable(product.getStock() > 0);

            // 计算小计
            if (product.getPrice() != null) {
                vo.setSubtotal(product.getPrice().multiply(java.math.BigDecimal.valueOf(cart.getQuantity())).doubleValue());
            } else {
                vo.setSubtotal(0.0);
            }
        } else {
            vo.setProductName("商品已下架");
            vo.setProductPrice(0.0);
            vo.setStock(0);
            vo.setAvailable(false);
            vo.setSubtotal(0.0);
        }

        return vo;
    }
}