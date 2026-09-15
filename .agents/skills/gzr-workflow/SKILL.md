---
name: gzr-workflow
description: Develop GZR NeoForge items and entities, review or clean GZR code, and prepare GZR GitHub commits. Use for work in C:/alex/code/gzr-mod-dev/guzhenren-template-1.21.1; excludes unrelated projects and general Minecraft questions.
---

# GZR 工作流

先读 `C:/alex/code/gzr-mod-dev/guzhenren-template-1.21.1/AGENTS.md`；Git 只在这个主模组根目录检查。父工作区不是仓库，排除目录不扫描。当前事实读磁盘，历史验证不能当本轮结果。

按任务只读需要的分支：

- 新物品、蛊或实体：[content.md](references/content.md)。
- 审查、修复或清理代码：[review.md](references/review.md)。
- 提交、推送、PR 或 CI：[git.md](references/git.md)。

先定位改动与调用者，后读领域参考；已知路径不重建全库地图。明确规格直接执行，只有未决玩法或无法从代码得出的高影响选择才问 Alex。

工具选择：文本/路径用 `rg`；Java 引用/继承关系优先用 IDEA 的 JetBrains MCP，IDEA 不可用时读磁盘与固定依赖源码，不为 GZR 启动 Serena；结构模式使用 `C:/Users/alex/.codex/tools/ast-grep/node_modules/.bin/ast-grep.cmd`，仅查询，写操作另行审阅。用短查询和限定路径控制输出，不串行调用多个等价索引。

版本资料用对应本地 JAR/源码与官方文档；Context7 是可选检索入口，缺少 1.21.1/GeckoLib 4 结果时不采用其他版本示例。GitHub 工具返回旧远程的新规范仓库名是重定向，保留现有 Git remote。

验证和文档收工统一遵循主模组 AGENTS 的对应工作流。没有行为变动的文档/规则不新造测试；客户端画面和手感始终由 Alex `runClient` 验收。工作流不授权新设计、跨仓修改、提交或外发消息。
