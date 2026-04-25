package com.nexus.service.impl;

import com.nexus.common.BusinessException;
import com.nexus.mapper.AddressMapper;
import com.nexus.mapper.MemberMapper;
import com.nexus.model.entity.Address;
import com.nexus.model.entity.Member;
import com.nexus.model.dto.AddressDTO;
import com.nexus.model.vo.AddressVO;
import com.nexus.service.AddressService;
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
 * 地址服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;
    private final MemberMapper memberMapper;

    @Override
    public List<AddressVO> getCurrentUserAddresses() {
        Long memberId = getCurrentMemberId();
        List<Address> addresses = addressMapper.selectByMemberId(memberId);

        return addresses.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AddressVO getAddressById(Long id) {
        Long memberId = getCurrentMemberId();

        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }

        // 检查是否属于当前用户
        if (!address.getMemberId().equals(memberId)) {
            throw new BusinessException("无权访问此地址");
        }

        return convertToVO(address);
    }

    @Override
    public AddressVO getDefaultAddress() {
        Long memberId = getCurrentMemberId();
        Address address = addressMapper.selectDefaultByMemberId(memberId);

        if (address == null) {
            // 如果没有默认地址，尝试获取第一个地址
            List<Address> addresses = addressMapper.selectByMemberId(memberId);
            if (!addresses.isEmpty()) {
                address = addresses.get(0);
            }
        }

        return address != null ? convertToVO(address) : null;
    }

    @Override
    @Transactional
    public Long addAddress(AddressDTO addressDTO) {
        Long memberId = getCurrentMemberId();

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setMemberId(memberId);
        address.setIsDefault(addressDTO.getIsDefault() != null ? addressDTO.getIsDefault() : false);
        address.setCreateTime(new Date());
        address.setUpdateTime(new Date());

        // 如果是第一条地址，自动设为默认
        Long count = addressMapper.countByMemberId(memberId);
        if (count == 0) {
            address.setIsDefault(true);
        }

        // 如果设置为默认，先清除其他默认
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressMapper.clearDefaultByMemberId(memberId);
        }

        addressMapper.insert(address);
        log.info("添加地址: memberId={}, addressId={}", memberId, address.getId());

        return address.getId();
    }

    @Override
    @Transactional
    public void updateAddress(Long id, AddressDTO addressDTO) {
        Long memberId = getCurrentMemberId();

        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }

        // 检查是否属于当前用户
        if (!address.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此地址");
        }

        BeanUtils.copyProperties(addressDTO, address);
        address.setId(id);
        address.setUpdateTime(new Date());

        // 如果设置为默认，先清除其他默认
        if (Boolean.TRUE.equals(addressDTO.getIsDefault())) {
            addressMapper.clearDefaultByMemberId(memberId);
            address.setIsDefault(true);
        }

        addressMapper.updateById(address);
        log.info("更新地址: addressId={}", id);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        Long memberId = getCurrentMemberId();

        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }

        // 检查是否属于当前用户
        if (!address.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此地址");
        }

        addressMapper.deleteById(id);
        log.info("删除地址: addressId={}", id);

        // 如果删除的是默认地址，设置第一个地址为默认
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> remaining = addressMapper.selectByMemberId(memberId);
            if (!remaining.isEmpty()) {
                addressMapper.setDefaultById(remaining.get(0).getId());
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long id) {
        Long memberId = getCurrentMemberId();

        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }

        // 检查是否属于当前用户
        if (!address.getMemberId().equals(memberId)) {
            throw new BusinessException("无权操作此地址");
        }

        // 清除其他默认，设置新默认
        addressMapper.clearDefaultByMemberId(memberId);
        addressMapper.setDefaultById(id);

        log.info("设置默认地址: addressId={}", id);
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
     * 将Address实体转换为AddressVO
     */
    private AddressVO convertToVO(Address address) {
        AddressVO vo = new AddressVO();
        BeanUtils.copyProperties(address, vo);

        // 组合完整地址
        vo.setFullAddress(String.format("%s %s %s %s",
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress()));

        return vo;
    }
}