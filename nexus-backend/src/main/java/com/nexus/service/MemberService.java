package com.nexus.service;

import com.nexus.model.entity.Member;
import com.nexus.model.dto.MemberDTO;
import com.nexus.model.vo.MemberVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface MemberService {

    /**
     * 用户注册
     */
    Long register(MemberDTO memberDTO);

    /**
     * 用户登录
     */
    String login(String username, String password);

    /**
     * 获取当前用户信息
     */
    MemberVO getCurrentMember();

    /**
     * 根据ID获取用户信息
     */
    MemberVO getMemberById(Long id);

    /**
     * 更新用户信息
     */
    MemberVO updateMember(Long id, MemberDTO memberDTO);

    /**
     * 删除用户
     */
    void deleteMember(Long id);

    /**
     * 获取所有用户列表
     */
    List<MemberVO> listAllMembers();

    /**
     * 分页查询用户
     */
    List<MemberVO> listMembersByPage(Integer pageNum, Integer pageSize);

    /**
     * 检查用户名是否存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查手机号是否存在
     */
    boolean isPhoneExists(String phone);

    /**
     * 检查邮箱是否存在
     */
    boolean isEmailExists(String email);
}