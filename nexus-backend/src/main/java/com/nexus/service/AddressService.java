package com.nexus.service;

import com.nexus.model.dto.AddressDTO;
import com.nexus.model.vo.AddressVO;
import java.util.List;

/**
 * 地址服务接口
 */
public interface AddressService {

    /**
     * 获取当前用户所有地址
     */
    List<AddressVO> getCurrentUserAddresses();

    /**
     * 根据ID获取地址
     */
    AddressVO getAddressById(Long id);

    /**
     * 获取当前用户默认地址
     */
    AddressVO getDefaultAddress();

    /**
     * 添加地址
     */
    Long addAddress(AddressDTO addressDTO);

    /**
     * 更新地址
     */
    void updateAddress(Long id, AddressDTO addressDTO);

    /**
     * 删除地址
     */
    void deleteAddress(Long id);

    /**
     * 设置默认地址
     */
    void setDefaultAddress(Long id);
}