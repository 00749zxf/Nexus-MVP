package com.nexus.agent.core;

/**
 * Agent执行器接口
 */
public interface AgentExecutor {

    /**
     * 执行Agent请求
     */
    AgentResponse executeWithLLM(AgentRequest request);

    /**
     * 生成响应（用于Mock模式）
     */
    String generateResponse(AgentRequest request, java.util.Map<String, Object> toolResults);
}