package com.nexus.agent.controller;

import com.nexus.agent.core.*;
import com.nexus.agent.tools.ToolRegistry;
import com.nexus.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Agent API控制器
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent API", description = "智能Agent对话接口")
public class AgentController {

    private final AgentEngine agentEngine;
    private final ToolRegistry toolRegistry;

    @PostMapping("/chat")
    @Operation(summary = "Agent对话", description = "与智能Agent进行对话")
    public Result<AgentResponse> chat(@RequestBody AgentRequest request) {
        AgentContext context = request.getContext();
        if (context == null) {
            context = new AgentContext();
            request.setContext(context);
        }

        log.info("Agent请求: sessionId={}, memberId={}, username={}, message={}",
            request.getSessionId(), context.getMemberId(), context.getUsername(),
            request.getMessage() != null ? request.getMessage().substring(0, Math.min(50, request.getMessage().length())) : "");

        // 从JWT获取用户身份（补充）
        if (context.getMemberId() == null && context.getUsername() == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                context.setUsername(auth.getName());
                log.info("从JWT获取用户: {}", auth.getName());
            }
        }

        AgentResponse response = agentEngine.execute(request);
        return Result.success(response);
    }

    @GetMapping("/types")
    @Operation(summary = "获取Agent类型", description = "获取可用的Agent类型列表")
    public Result<AgentTypeInfo[]> getAgentTypes() {
        AgentTypeInfo[] types = Arrays.stream(AgentRequest.AgentType.values())
                .map(t -> new AgentTypeInfo(t.name(), t.getDescription()))
                .toArray(AgentTypeInfo[]::new);
        return Result.success(types);
    }

    @GetMapping("/tools")
    @Operation(summary = "获取工具列表", description = "获取Agent可使用的工具列表")
    public Result<List<String>> getTools() {
        List<String> tools = toolRegistry.getAvailableToolNames();
        return Result.success(tools);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AgentTypeInfo {
        private String name;
        private String description;
    }
}