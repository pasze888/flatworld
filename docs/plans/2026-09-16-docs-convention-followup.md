# 文档落点规范：迁移记录

依据工作区根 `AGENTS.md` §7（文档落点与协作规范）。

## 已完成

- **2026-09-16 结构迁移**：`docs/KNOWLEDGE.md` → `docs/reference/dimension-registration.md`
  + `docs/troubleshooting.md`。
- **2026-09-26 README 双语拆分**：`README.md` 改为英文源，新增 `README.zh-CN.md` 中文同步，
  顶部加 `[English](README.md) | [简体中文](README.zh-CN.md)` 语言切换行。
  原先留在 README 里的「技术要点」（维度注册三键、datagen 流程、手写 JSON 的原因、
  边缘发黑的雾根因、传送与安全落点）按 §7.3 归位到 `docs/design/` 与 `docs/reference/`——
  这些内容在 `docs/reference/dimension-registration.md` 中已被完整覆盖（边缘发黑一处更详细），
  无信息丢失。

## 待办

（无）

## 依据

- §7.3：README 只承载简介、安装、快速开始、配置、常用命令、文档链接；`README.md` 为英文源，
  `README.zh-CN.md` 为中文同步——改一必同步另一，收尾逐项核对标题层级、顺序、代码块数量、
  链接/图片路径、命令、配置项。

