package com.nexus.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 用户视图对象
 */
@Data
public class MemberVO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}