package com.nexus.agent.tools;

import java.util.List;
import java.util.Map;

/**
 * Agent工具接口
 * 所有Agent可调用的工具都需要实现此接口
 */
public interface Tool {

    /**
     * 工具名称（唯一标识）
     */
    String getName();

    /**
     * 工具描述（给LLM看的，描述工具的用途）
     */
    String getDescription();

    /**
     * 执行工具
     */
    ToolResult execute(Map<String, Object> params);

    /**
     * 获取参数Schema
     */
    ToolSchema getSchema();

    /**
     * 工具是否安全（只读工具为安全，操作类工具需用户确认）
     */
    default boolean isSafe() {
        return true;
    }
}