package com.nexus.mapper;

import com.nexus.model.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 地址Mapper接口
 */
@Mapper
public interface AddressMapper {

    /**
     * 插入地址
     */
    int insert(Address address);

    /**
     * 根据ID更新地址
     */
    int updateById(Address address);

    /**
     * 根据ID删除地址
     */
    int deleteById(Long id);

    /**
     * 根据ID查询地址
     */
    Address selectById(Long id);

    /**
     * 根据会员ID查询所有地址
     */
    List<Address> selectByMemberId(Long memberId);

    /**
     * 根据会员ID查询默认地址
     */
    Address selectDefaultByMemberId(Long memberId);

    /**
     * 清除会员的所有默认地址
     */
    void clearDefaultByMemberId(Long memberId);

    /**
     * 设置指定地址为默认
     */
    void setDefaultById(Long id);

    /**
     * 统计会员地址数量
     */
    Long countByMemberId(Long memberId);
}