package com.nexus.controller;

import com.nexus.common.Result;
import com.nexus.model.dto.MemberDTO;
import com.nexus.model.vo.MemberVO;
import com.nexus.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
@Validated
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody MemberDTO memberDTO) {
        Long memberId = memberService.register(memberDTO);
        return Result.success("注册成功", memberId);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username,
                                @RequestParam String password) {
        String token = memberService.login(username, password);
        return Result.success("登录成功", token);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<MemberVO> getCurrentMember() {
        MemberVO memberVO = memberService.getCurrentMember();
        return Result.success(memberVO);
    }

    @Operation(summary = "根据ID获取用户信息")
    @GetMapping("/{id}")
    public Result<MemberVO> getMemberById(@PathVariable Long id) {
        MemberVO memberVO = memberService.getMemberById(id);
        return Result.success(memberVO);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<MemberVO> updateMember(@PathVariable Long id,
                                     @Valid @RequestBody MemberDTO memberDTO) {
        MemberVO updatedMember = memberService.updateMember(id, memberDTO);
        return Result.success("更新成功", updatedMember);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "获取所有用户列表")
    @GetMapping
    public Result<List<MemberVO>> listAllMembers() {
        List<MemberVO> members = memberService.listAllMembers();
        return Result.success(members);
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<List<MemberVO>> listMembersByPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<MemberVO> members = memberService.listMembersByPage(pageNum, pageSize);
        return Result.success(members);
    }

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/check/username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = memberService.isUsernameExists(username);
        return Result.success(exists);
    }

    @Operation(summary = "检查手机号是否存在")
    @GetMapping("/check/phone")
    public Result<Boolean> checkPhone(@RequestParam String phone) {
        boolean exists = memberService.isPhoneExists(phone);
        return Result.success(exists);
    }

    @Operation(summary = "检查邮箱是否存在")
    @GetMapping("/check/email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = memberService.isEmailExists(email);
        return Result.success(exists);
    }
}