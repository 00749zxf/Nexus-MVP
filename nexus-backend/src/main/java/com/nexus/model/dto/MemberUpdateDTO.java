package com.nexus.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemberUpdateDTO {

    @Size(min = 3, max = 64, message = "用户名长度必须在3-64个字符之间")
    private String username;

    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式不正确")
    private String email;

    private String avatar;
}
