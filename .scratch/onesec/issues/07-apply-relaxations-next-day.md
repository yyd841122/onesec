# 07: 让放宽规则在次日生效

**What to build:** 允许使用者立即新增或收紧自律规则，但把增加每日额度、删除强限制或关闭保护记录为待生效变更，直到下一个本地自然日才应用。

**Blocked by:** 05: 额度耗尽后执行强限制拦截

**Status:** completed

- [x] 新增受限应用或降低每日额度可立即生效。
- [x] 将弱限制改为强限制可立即生效。
- [x] 增加每日额度会创建待生效变更，当前日期的有效规则保持不变。
- [x] 删除强限制或关闭保护会创建待生效变更，当前日期仍继续保护。
- [x] 界面清楚区分当前生效规则与次日即将生效的变更。
- [x] 到达下一个本地零点后，待生效变更恰好应用一次并成为当前规则。
- [x] 应用或手机重启不会提前应用、重复应用或丢失待生效变更。
- [x] 限制决策模块测试覆盖立即收紧、延迟放宽、跨日应用和时钟跨越零点的行为。

## Comments

- 2026-08-21: Implemented framework-free rule-change decisions, persisted pending relaxations, next-local-date reconciliation, immediate tightening, delayed allowance increases/removal/protection disabling, and separate current/pending UI sections. Unit tests, Android persistence/UI test compilation, lint, and the full Gradle check pass.
