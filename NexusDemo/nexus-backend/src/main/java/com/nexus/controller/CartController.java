package com.nexus.controller;

import com.nexus.common.Result;
import com.nexus.model.dto.CartDTO;
import com.nexus.model.vo.CartVO;
import com.nexus.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 购物车控制器
 */
@Tag(name = "购物车管理", description = "购物车添加、删除、更新、查询")
@Validated
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "获取购物车列表")
    @GetMapping
    public Result<List<CartVO>> getCart() {
        List<CartVO> cartItems = cartService.getCurrentUserCart();
        return Result.success(cartItems);
    }

    @Operation(summary = "添加到购物车")
    @PostMapping
    public Result<Long> addToCart(@Valid @RequestBody CartDTO cartDTO) {
        Long cartItemId = cartService.addToCart(cartDTO);
        return Result.success("添加成功", cartItemId);
    }

    @Operation(summary = "更新购物车商品数量")
    @PutMapping("/{itemId}")
    public Result<Void> updateCartItem(@PathVariable Long itemId,
                                       @RequestParam Integer quantity) {
        cartService.updateQuantity(itemId, quantity);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{itemId}")
    public Result<Void> removeCartItem(@PathVariable Long itemId) {
        cartService.removeCartItem(itemId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clearCart() {
        cartService.clearCart();
        return Result.success("清空成功", null);
    }

    @Operation(summary = "更新商品选择状态")
    @PutMapping("/{itemId}/selection")
    public Result<Void> updateItemSelection(@PathVariable Long itemId,
                                            @RequestParam Boolean selected) {
        cartService.updateSelection(itemId, selected);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "批量更新选择状态")
    @PutMapping("/batch-selection")
    public Result<Void> batchUpdateSelection(@RequestParam @NotEmpty List<Long> itemIds,
                                             @RequestParam Boolean selected) {
        cartService.batchUpdateSelection(itemIds, selected);
        return Result.success("更新成功", null);
    }
}