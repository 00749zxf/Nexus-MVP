package com.nexus.controller;

import com.nexus.common.Result;
import com.nexus.model.dto.AddressDTO;
import com.nexus.model.vo.AddressVO;
import com.nexus.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址控制器
 */
@Slf4j
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "地址管理", description = "用户收货地址相关接口")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "获取所有地址", description = "获取当前用户的所有收货地址")
    public Result<List<AddressVO>> getAddresses() {
        List<AddressVO> addresses = addressService.getCurrentUserAddresses();
        return Result.success(addresses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个地址", description = "根据ID获取地址详情")
    public Result<AddressVO> getAddressById(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        AddressVO address = addressService.getAddressById(id);
        return Result.success(address);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认地址", description = "获取当前用户的默认收货地址")
    public Result<AddressVO> getDefaultAddress() {
        AddressVO address = addressService.getDefaultAddress();
        return Result.success(address);
    }

    @PostMapping
    @Operation(summary = "添加地址", description = "添加新的收货地址")
    public Result<Long> addAddress(@Valid @RequestBody AddressDTO addressDTO) {
        Long addressId = addressService.addAddress(addressDTO);
        return Result.success("添加成功", addressId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地址", description = "更新收货地址信息")
    public Result<Void> updateAddress(
            @Parameter(description = "地址ID") @PathVariable Long id,
            @Valid @RequestBody AddressDTO addressDTO) {
        addressService.updateAddress(id, addressDTO);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址", description = "删除收货地址")
    public Result<Void> deleteAddress(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        addressService.deleteAddress(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认地址", description = "将指定地址设为默认收货地址")
    public Result<Void> setDefaultAddress(
            @Parameter(description = "地址ID") @PathVariable Long id) {
        addressService.setDefaultAddress(id);
        return Result.success("设置成功", null);
    }
}