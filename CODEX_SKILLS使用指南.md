# Matt Pocock Skills：Codex 项目级使用指南

## 1. 安装结果

已将 `mattpocock/skills` 仓库中识别到的 **35 个技能**全部安装到当前项目，安装范围仅限本项目。

- 项目根目录：`/home/youyd/projects/codex/onesec`
- 技能目录：`/home/youyd/projects/codex/onesec/.agents/skills/`
- 安装锁文件：`/home/youyd/projects/codex/onesec/skills-lock.json`
- 安装范围：Project（项目级）
- 目标 Agent：Codex
- 全局目录：未安装到 `~/.codex/skills/`

每个技能各有一个目录，入口文件通常是：

```text
.agents/skills/<技能名>/SKILL.md
```

`skills-lock.json` 记录了来源仓库、仓库内路径和内容哈希，便于后续更新与恢复。

## 2. 让 Codex 识别新技能

安装后的技能会在 **下一轮对话或新会话**中可用。为了避免当前会话仍使用安装前的技能清单，推荐：

1. 保持 Codex 的工作目录为 `/home/youyd/projects/codex/onesec`。
2. 新建一个 Codex 会话，或结束当前任务后再发起下一条消息。
3. 明确写出技能名，确保触发指定技能。

如果离开本项目目录，这批项目级技能不应被视为其他项目的技能。

## 3. 如何调用技能

在 Codex 中，推荐用 `$技能名` 显式调用；也可以直接用自然语言提出与技能描述匹配的任务，由 Codex 自动选择。

显式调用示例：

```text
请使用 $ask-matt，帮我判断这个需求应该走哪个工作流：我要给现有 API 增加批量导入功能。
```

```text
请使用 $diagnosing-bugs，诊断为什么这个测试偶发超时。先定位原因，不要直接修改代码。
```

```text
请使用 $tdd，以测试驱动方式实现用户注册邮箱去重。
```

```text
请使用 $code-review，审查当前工作区相对于 main 的改动。
```

自然语言调用示例：

```text
请测试驱动地实现这个功能，并先写一个失败的集成测试。
```

这通常会触发 `tdd`，但想要稳定、可预测地测试某个技能时，仍建议显式写 `$tdd`。

仓库中的文档有时使用 `/tdd`、`/implement` 这类写法，这是跨 Agent 的命令式表达。在 Codex 对话中优先使用 `$tdd`、`$implement` 或“请使用 tdd 技能”。

## 4. 第一次推荐怎么用

### 4.1 不确定该选哪个技能

先用路由技能：

```text
请使用 $ask-matt。我的目标是：<描述目标>。请告诉我应该使用哪些技能、顺序是什么，以及每一步会产出什么。
```

### 4.2 在真实代码仓库中使用工程工作流

工程技能约定先运行一次初始化技能：

```text
请使用 $setup-matt-pocock-skills，为当前仓库配置工程技能所需的 issue tracker、triage 标签和 domain docs。先展示建议，得到我确认后再写文件。
```

它可能需要你选择：

- Issue 放 GitHub、GitLab，还是本地 `.scratch/` Markdown 文件；
- 是否采用默认 triage 标签；
- 使用 `AGENTS.md` 还是 `CLAUDE.md`（当两者都不存在时）。

注意：当前 `onesec` 目录安装时还不是 Git 仓库，只用于测试技能完全没问题；但 `code-review`、`resolving-merge-conflicts`、`implement` 等工程技能在真实 Git 仓库中才更有意义。

### 4.3 一个完整的功能开发流程

典型顺序如下：

```text
$grill-with-docs → $to-spec → $to-tickets → $implement → $code-review
```

- `$grill-with-docs`：通过追问澄清需求，并把领域知识/决策写进项目文档。
- `$to-spec`：把已经讨论清楚的内容整理成规格。
- `$to-tickets`：把规格拆成可执行、有依赖关系的任务。
- `$implement`：按任务实现，内部会使用 TDD 思路并在结束前审查。
- `$code-review`：相对某个明确基准，同时检查项目规范和需求符合度。

小功能不必走完整流程。例如：

```text
请使用 $implement，根据下面这份已明确的小需求直接实现：……
```

### 4.4 常见问题对应技能

| 场景 | 推荐技能 | 示例说法 |
| --- | --- | --- |
| 不知道选什么流程 | `ask-matt` | `请使用 $ask-matt 帮我选流程` |
| 深挖并澄清一个想法 | `grill-with-docs` | `用 $grill-with-docs 追问我并记录决策` |
| 疑难 Bug / 性能回退 | `diagnosing-bugs` | `用 $diagnosing-bugs 先建立稳定复现` |
| 测试驱动开发 | `tdd` | `用 $tdd 实现这个行为` |
| 审查分支或工作区改动 | `code-review` | `用 $code-review 审查相对 main 的变化` |
| 探索 UI 或状态模型 | `prototype` | `用 $prototype 做一个可丢弃原型` |
| 调研官方资料并留档 | `research` | `用 $research 调研并生成带引用的 Markdown` |
| 解决 merge/rebase 冲突 | `resolving-merge-conflicts` | `用 $resolving-merge-conflicts 按双方意图解决冲突` |
| 改善模块边界 | `codebase-design` | `用 $codebase-design 设计更深的模块接口` |
| 大型、模糊、多会话项目 | `wayfinder` | `用 $wayfinder 建立决策地图` |
| 保存当前上下文给下一会话 | `handoff` | `用 $handoff 生成交接文档` |
| 学习一个概念 | `teach` | `用 $teach 带我分阶段学习领域建模` |

## 5. 查看、更新和卸载

以下命令都应在项目根目录运行。

查看本项目已安装技能：

```bash
npx skills@latest list
```

以 JSON 查看：

```bash
npx skills@latest list --json
```

只更新项目技能：

```bash
npx skills@latest update --project -y
```

卸载某个项目技能：

```bash
npx skills@latest remove <技能名> -y
```

卸载本项目的全部技能：

```bash
npx skills@latest remove --all
```

不要给这些命令加 `--global` 或 `-g`，否则操作目标会变成用户级全局技能。

如果要在另一台机器或干净环境中按锁文件恢复，可在项目根目录尝试：

```bash
npx skills@latest experimental_install
```

## 6. 如何阅读和评估技能

技能不是普通依赖库，而是会影响 Agent 工作方式的指令。使用前可以直接查看：

```text
.agents/skills/<技能名>/SKILL.md
```

重点检查：

- 触发条件和前置条件；
- 是否会写文件、提交代码、创建 issue 或调用外部服务；
- 是否要求子 Agent、GitHub CLI 或其他工具；
- 是否包含与你项目约定冲突的流程。

安装器的扫描结果中，`code-review` 和 `claude-handoff` 被一项扫描标记为中风险；`code-review` 与 `writing-shape` 也被另一项扫描标为高风险。扫描结果只是提醒，不等于确认恶意。首次测试时建议明确限制权限和动作，例如：

```text
请使用 $code-review，只做只读审查，不修改文件、不提交、不调用外部服务。
```

## 7. 建议的学习练习

按下面顺序体验，容易看出每类技能的价值：

1. `$ask-matt`：给它一个真实目标，让它推荐路线。
2. `$grill-me`：拿一个不涉及当前仓库的点子接受追问。
3. `$teach`：让它在当前目录建立一个小型学习计划。
4. 在一个真实 Git 项目中先运行 `$setup-matt-pocock-skills`。
5. 用 `$diagnosing-bugs` 处理一个可复现 Bug，或用 `$tdd` 实现一个小行为。
6. 最后用 `$code-review` 审查相对于明确基准的改动。

每次测试都建议在提示词中写清：目标、允许的动作、禁止的动作、验收标准。例如：

```text
请使用 $tdd。
目标：实现邮箱地址大小写不敏感的去重。
允许：修改 src/ 和 tests/。
禁止：安装新依赖、提交 Git commit。
验收：新增测试先失败再通过，并运行现有测试套件。
```

