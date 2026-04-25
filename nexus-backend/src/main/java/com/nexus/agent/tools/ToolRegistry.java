package com.nexus.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心
 * 管理所有Agent可调用的工具
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> toolMap = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void register(Tool tool) {
        if (toolMap.containsKey(tool.getName())) {
            log.warn("工具已存在，将被覆盖: {}", tool.getName());
        }
        toolMap.put(tool.getName(), tool);
        log.info("注册工具: {} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 获取所有工具
     */
    public Collection<Tool> getAllTools() {
        return toolMap.values();
    }

    /**
     * 获取指定名称的工具列表
     */
    public List<Tool> getTools(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return names.stream()
                .map(toolMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 执行工具
     */
    public ToolResult execute(String toolName, Map<String, Object> params) {
        Tool tool = toolMap.get(toolName);
        if (tool == null) {
            return ToolResult.error("工具不存在: " + toolName);
        }
        try {
            return tool.execute(params);
        } catch (Exception e) {
            log.error("工具执行失败: {}, params: {}", toolName, params, e);
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return toolMap.containsKey(name);
    }

    /**
     * 获取所有可用工具名称
     */
    public List<String> getAvailableToolNames() {
        return new ArrayList<>(toolMap.keySet());
    }
}