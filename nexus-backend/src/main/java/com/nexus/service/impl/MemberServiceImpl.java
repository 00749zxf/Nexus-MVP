package com.nexus.service.impl;

import com.nexus.common.BusinessException;
import com.nexus.mapper.MemberMapper;
import com.nexus.model.entity.Member;
import com.nexus.model.dto.MemberDTO;
import com.nexus.model.vo.MemberVO;
import com.nexus.service.MemberService;
import com.nexus.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public Long register(MemberDTO memberDTO) {
        // 检查用户名是否已存在
        if (isUsernameExists(memberDTO.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否已存在
        if (memberDTO.getPhone() != null && isPhoneExists(memberDTO.getPhone())) {
            throw new BusinessException("手机号已存在");
        }

        // 检查邮箱是否已存在
        if (memberDTO.getEmail() != null && isEmailExists(memberDTO.getEmail())) {
            throw new BusinessException("邮箱已存在");
        }

        // 创建用户实体
        Member member = new Member();
        BeanUtils.copyProperties(memberDTO, member);

        // 密码加密
        member.setPassword(passwordEncoder.encode(memberDTO.getPassword()));
        member.setStatus(1); // 默认启用
        member.setCreateTime(new Date());
        member.setUpdateTime(new Date());

        // 插入数据库
        int result = memberMapper.insert(member);
        if (result <= 0) {
            throw new BusinessException("用户注册失败");
        }

        log.info("用户注册成功: username={}, id={}", member.getUsername(), member.getId());
        return member.getId();
    }

    @Override
    public String login(String username, String password) {
        // 根据用户名查询用户
        log.debug("尝试登录: username={}", username);
        Member member = memberMapper.selectByUsername(username);
        log.debug("查询结果: member={}", member);
        if (member == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (member.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        // 验证密码
        log.debug("密码验证: username={}, storedHash={}, passwordLength={}", username, member.getPassword(), password != null ? password.length() : 0);
        boolean matches = passwordEncoder.matches(password, member.getPassword());
        log.debug("密码匹配结果: {}", matches);
        if (!matches) {
            throw new BusinessException("用户名或密码错误");
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(username);
        log.info("用户登录成功: username={}", username);
        return token;
    }

    @Override
    public MemberVO getCurrentMember() {
        // 从SecurityContext获取当前认证的用户名
        String username = getCurrentUsername();
        if (username == null) {
            throw new BusinessException("用户未登录");
        }

        Member member = memberMapper.selectByUsername(username);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        return convertToVO(member);
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

    @Override
    public MemberVO getMemberById(Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        return convertToVO(member);
    }

    @Override
    @Transactional
    public MemberVO updateMember(Long id, MemberDTO memberDTO) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查用户名是否重复（排除自己）
        if (memberDTO.getUsername() != null && !member.getUsername().equals(memberDTO.getUsername())) {
            if (isUsernameExists(memberDTO.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
            member.setUsername(memberDTO.getUsername());
        }

        // 检查手机号是否重复
        if (memberDTO.getPhone() != null && !memberDTO.getPhone().equals(member.getPhone())) {
            if (isPhoneExists(memberDTO.getPhone())) {
                throw new BusinessException("手机号已存在");
            }
            member.setPhone(memberDTO.getPhone());
        }

        // 检查邮箱是否重复
        if (memberDTO.getEmail() != null && !memberDTO.getEmail().equals(member.getEmail())) {
            if (isEmailExists(memberDTO.getEmail())) {
                throw new BusinessException("邮箱已存在");
            }
            member.setEmail(memberDTO.getEmail());
        }

        // 更新头像（如果有）
        if (memberDTO.getAvatar() != null) {
            member.setAvatar(memberDTO.getAvatar());
        }

        // 更新密码（如果有）
        if (memberDTO.getPassword() != null && !memberDTO.getPassword().isEmpty()) {
            member.setPassword(passwordEncoder.encode(memberDTO.getPassword()));
        }

        // 更新状态（如果有）
        if (memberDTO.getStatus() != null) {
            member.setStatus(memberDTO.getStatus());
        }

        member.setUpdateTime(new Date());
        int result = memberMapper.updateById(member);
        if (result <= 0) {
            throw new BusinessException("更新用户信息失败");
        }

        log.info("用户信息更新成功: id={}", id);

        // 返回更新后的用户信息
        return getMemberById(id);
    }

    @Override
    @Transactional
    public void deleteMember(Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        int result = memberMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException("删除用户失败");
        }

        log.info("用户删除成功: id={}", id);
    }

    @Override
    public List<MemberVO> listAllMembers() {
        List<Member> members = memberMapper.selectAll();
        return members.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberVO> listMembersByPage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        Integer offset = (pageNum - 1) * pageSize;
        List<Member> members = memberMapper.selectPage(offset, pageSize);

        return members.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isUsernameExists(String username) {
        return memberMapper.selectByUsername(username) != null;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        return memberMapper.selectByPhone(phone) != null;
    }

    @Override
    public boolean isEmailExists(String email) {
        return memberMapper.selectByEmail(email) != null;
    }

    /**
     * 将Member实体转换为MemberVO
     */
    private MemberVO convertToVO(Member member) {
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(member, vo);
        // 可以在这里添加额外的转换逻辑
        return vo;
    }
}