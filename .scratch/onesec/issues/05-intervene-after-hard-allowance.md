# 05: 额度耗尽后执行强限制拦截

**What to build:** 当使用者尝试进入已经耗尽每日额度的强限制应用时，OneSec 应通过统一的限制决策模块作出强限制拦截决定并展示拦截页，使使用者能够理解原因并返回桌面。

**Blocked by:** 04: 显示受限应用的今日实际用时

**Status:** completed

- [x] 限制决策模块通过单一 interface 接收当前时间、受限应用、当日用量、规则和保护状态，并返回用户可观察的保护决策。
- [x] 每日额度尚未耗尽时，受限应用正常打开且不出现额外摩擦。
- [x] 每日额度达到或超过上限时，进入强限制应用会触发拦截。
- [x] 拦截页显示受限应用、今日已用时间和下次本地零点重置时间。
- [x] 拦截页提供清晰的返回桌面操作，不显示醒目的即时延长入口。
- [x] 缺失核心权限时返回保护失效，而不是假装已经执行拦截。
- [x] 限制决策模块不依赖 Android framework，并以参数化测试覆盖额度前、额度边界、额度后和权限失效。
- [x] Android 集成测试验证前台应用事件能触发决定，并将强限制决定转换为可见的拦截体验。

## Comments

- 2026-08-21: Implemented the framework-free protection decision engine, accessibility foreground monitoring, hard-restriction intervention Activity, local-midnight reset display, and return-home action. Automated unit and Android integration coverage was added; no Android device was connected for an instrumentation run.
- 2026-08-21: Fixed intermittent re-entry after an intervention by persisting an exhausted-allowance latch per restricted app and local date, so temporarily stale UsageStats data cannot relax a hard restriction. The regression and persistence tests passed on PCAM00.
