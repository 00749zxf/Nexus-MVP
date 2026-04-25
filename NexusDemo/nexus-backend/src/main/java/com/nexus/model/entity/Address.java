package com.nexus.model.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 收货地址实体类
 */
@Data
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区/县
     */
    private String district;

    /**
     * 详细地址
     */
    private String detailAddress;

    /**
     * 是否默认地址
     */
    private Boolean isDefault;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}