package com.nexus.agent.tools;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 工具参数Schema
 */
@Data
public class ToolSchema implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 参数定义列表
     */
    private List<ParamDef> params;

    @Data
    public static class ParamDef implements Serializable {
        /**
         * 参数名
         */
        private String name;

        /**
         * 参数类型
         */
        private String type;  // string, number, boolean, object, array

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必填
         */
        private boolean required;

        /**
         * 默认值
         */
        private Object defaultValue;

        public static ParamDef of(String name, String type, String description, boolean required) {
            ParamDef def = new ParamDef();
            def.setName(name);
            def.setType(type);
            def.setDescription(description);
            def.setRequired(required);
            return def;
        }

        public static ParamDef of(String name, String type, String description, boolean required, Object defaultValue) {
            ParamDef def = of(name, type, description, required);
            def.setDefaultValue(defaultValue);
            return def;
        }
    }
}