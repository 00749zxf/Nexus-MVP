package com.nexus.security;

import com.nexus.mapper.MemberMapper;
import com.nexus.model.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 自定义用户详情服务
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberMapper memberMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberMapper.selectByUsername(username);
        if (member == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (member.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 暂时使用简单的权限，后续可以根据需要扩展
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        return new User(
                member.getUsername(),
                member.getPassword(),
                Collections.singletonList(authority)
        );
    }
}