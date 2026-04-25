package com.nexus.mapper;

import com.nexus.model.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户Mapper接口
 */
@Mapper
public interface MemberMapper {

    /**
     * 插入用户
     */
    int insert(Member member);

    /**
     * 根据ID更新用户
     */
    int updateById(Member member);

    /**
     * 根据ID删除用户
     */
    int deleteById(Long id);

    /**
     * 根据ID查询用户
     */
    Member selectById(Long id);

    /**
     * 根据用户名查询用户
     */
    Member selectByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    Member selectByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    Member selectByEmail(String email);

    /**
     * 查询所有用户
     */
    List<Member> selectAll();

    /**
     * 分页查询用户
     */
    List<Member> selectPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计用户数量
     */
    Long count();
}