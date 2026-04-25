# CLAUDE.md — 项目行为指令

## 记忆优先规则

**每次回答用户问题前，必须先读取记忆文件：**

1. 先读取 `C:\Users\ASUS\.claude\projects\E--project-copy2\memory\MEMORY.md` 获取记忆索引
2. 根据索引读取相关记忆文件获取上下文
3. 结合记忆内容回答用户问题
4. 如果记忆内容与当前实际情况冲突，以实际观察为准，并更新记忆

## 何时写入记忆

- 完成重要功能开发后，更新对应记忆文件
- 发现记忆过时时，更新而非删除
- 用户明确要求记忆某事时，立即写入

## 记忆文件路径

- 索引: `E:\project\copy2\.claude\projects\E--project-copy2\memory\MEMORY.md`
- 记忆文件: `E:\project\copy2\.claude\projects\E--project-copy2\memory\*.md`

## 当前项目: Nexus电商平台

- 后端: Spring Boot (端口8083)
- 前端: Vue 3 (端口5174)
- Agent系统已集成DeepSeek
