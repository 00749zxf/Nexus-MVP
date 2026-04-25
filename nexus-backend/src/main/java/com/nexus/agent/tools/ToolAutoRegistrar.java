package com.nexus.agent.tools;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具自动注册器
 * 在应用启动时自动注册所有工具到ToolRegistry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAutoRegistrar {

    private final ToolRegistry toolRegistry;
    private final List<Tool> tools;  // Spring自动注入所有Tool实现

    @PostConstruct
    public void registerAllTools() {
        log.info("开始注册Agent工具...");
        for (Tool tool : tools) {
            toolRegistry.register(tool);
        }
        log.info("Agent工具注册完成，共注册 {} 个工具", tools.size());
    }
}