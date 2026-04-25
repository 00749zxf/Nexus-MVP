package com.nexus.agent.prompts;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * Prompt模板
 */
@Data
public class PromptTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板类型
     */
    private String type;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 模板变量（用于动态替换）
     */
    private Map<String, String> variables;

    /**
     * 渲染模板（替换变量）
     */
    public String render(Map<String, String> contextVars) {
        if (systemPrompt == null) return "";

        String result = systemPrompt;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = contextVars != null ? contextVars.getOrDefault(entry.getKey(), entry.getValue()) : entry.getValue();
                result = result.replace(placeholder, value);
            }
        }
        return result;
    }
}