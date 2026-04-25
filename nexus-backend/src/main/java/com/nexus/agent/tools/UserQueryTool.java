package com.nexus.agent.tools;

import com.nexus.mapper.MemberMapper;
import com.nexus.model.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户查询工具
 * 提供用户信息查询能力给Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserQueryTool implements Tool {

    private final MemberMapper memberMapper;

    @Override
    public String getName() {
        return "queryUser";
    }

    @Override
    public String getDescription() {
        return "查询用户信息，支持按用户ID、用户名查询。返回用户基本信息。";
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Long memberId = getLongParam(params, "memberId");
        String username = getStringParam(params, "username");

        try {
            if (memberId != null) {
                Member member = memberMapper.selectById(memberId);
                if (member == null) {
                    return ToolResult.error("用户不存在: " + memberId);
                }
                return ToolResult.ok(toUserInfo(member));
            }

            if (username != null) {
                Member member = memberMapper.selectByUsername(username);
                if (member == null) {
                    return ToolResult.error("用户不存在: " + username);
                }
                return ToolResult.ok(toUserInfo(member));
            }

            return ToolResult.error("请提供查询条件：memberId 或 username");

        } catch (Exception e) {
            log.error("用户查询失败", e);
            return ToolResult.error("用户查询失败: " + e.getMessage());
        }
    }

    @Override
    public ToolSchema getSchema() {
        ToolSchema schema = new ToolSchema();
        schema.setParams(List.of(
                ToolSchema.ParamDef.of("memberId", "number", "用户ID", false),
                ToolSchema.ParamDef.of("username", "string", "用户名", false)
        ));
        return schema;
    }

    private UserInfo toUserInfo(Member member) {
        UserInfo info = new UserInfo();
        info.setId(member.getId());
        info.setUsername(member.getUsername());
        info.setPhone(member.getPhone());
        info.setEmail(member.getEmail());
        info.setStatus(member.getStatus());
        info.setStatusDesc(member.getStatus() == 1 ? "正常" : "禁用");
        return info;
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }

    private String getStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    @lombok.Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String phone;
        private String email;
        private Integer status;
        private String statusDesc;
    }
}