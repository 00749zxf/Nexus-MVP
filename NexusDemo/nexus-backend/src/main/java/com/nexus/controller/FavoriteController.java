package com.nexus.controller;

import com.nexus.common.Result;
import com.nexus.model.dto.FavoriteDTO;
import com.nexus.model.vo.FavoriteVO;
import com.nexus.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器
 */
@Slf4j
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "收藏管理", description = "用户收藏商品相关接口")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "获取所有收藏", description = "获取当前用户的所有收藏商品")
    public Result<List<FavoriteVO>> getFavorites() {
        List<FavoriteVO> favorites = favoriteService.getCurrentUserFavorites();
        return Result.success(favorites);
    }

    @GetMapping("/page")
    @Operation(summary = "分页获取收藏", description = "分页获取当前用户的收藏商品")
    public Result<List<FavoriteVO>> getFavoritesPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        List<FavoriteVO> favorites = favoriteService.getCurrentUserFavoritesPage(pageNum, pageSize);
        return Result.success(favorites);
    }

    @PostMapping
    @Operation(summary = "添加收藏", description = "收藏指定商品")
    public Result<Long> addFavorite(@Valid @RequestBody FavoriteDTO favoriteDTO) {
        Long favoriteId = favoriteService.addFavorite(favoriteDTO);
        return Result.success("收藏成功", favoriteId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除收藏", description = "根据收藏ID删除收藏")
    public Result<Void> removeFavorite(
            @Parameter(description = "收藏ID") @PathVariable Long id) {
        favoriteService.removeFavorite(id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "按商品ID删除收藏", description = "根据商品ID取消收藏")
    public Result<Void> removeFavoriteByProductId(
            @Parameter(description = "商品ID") @PathVariable Long productId) {
        favoriteService.removeFavoriteByProductId(productId);
        return Result.success("取消收藏成功", null);
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "检查是否收藏", description = "检查是否已收藏指定商品")
    public Result<Boolean> checkFavorite(
            @Parameter(description = "商品ID") @PathVariable Long productId) {
        boolean isFavorite = favoriteService.isFavorite(productId);
        return Result.success(isFavorite);
    }

    @GetMapping("/count")
    @Operation(summary = "获取收藏数量", description = "获取当前用户的收藏数量")
    public Result<Long> getFavoriteCount() {
        Long count = favoriteService.getFavoriteCount();
        return Result.success(count);
    }

    @PostMapping("/move-from-cart")
    @Operation(summary = "从购物车移入收藏", description = "将购物车商品移入收藏并删除购物车项")
    public Result<Long> moveFromCart(
            @Parameter(description = "商品ID") @RequestParam Long productId,
            @Parameter(description = "购物车项ID") @RequestParam Long cartItemId) {
        Long favoriteId = favoriteService.moveFromCart(productId, cartItemId);
        return Result.success("移入收藏成功", favoriteId);
    }
}