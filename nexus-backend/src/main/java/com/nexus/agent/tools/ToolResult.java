package com.nexus.agent.tools;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 工具执行结果
 */
@Data
public class ToolResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private Object data;
    private String message;
    private List<String> suggestions;

    public static ToolResult ok(Object data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static ToolResult ok(Object data, String message) {
        ToolResult result = ok(data);
        result.setMessage(message);
        return result;
    }

    public static ToolResult error(String message) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static ToolResult error(String message, Object data) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}