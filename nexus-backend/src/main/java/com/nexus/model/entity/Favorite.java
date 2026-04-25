package com.nexus.model.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户收藏实体类
 */
@Data
public class Favorite implements Serializable {
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
     * 商品ID
     */
    private Long productId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}