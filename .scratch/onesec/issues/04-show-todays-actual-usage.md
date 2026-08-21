# 04: 显示受限应用的今日实际用时

**What to build:** 读取受限应用在当前本地自然日的前台使用事件，在今日概览中显示已用时间和剩余每日额度，并明确区分可靠数据与权限失效状态。

**Blocked by:** 02: 完成权限引导与保护状态检测; 03: 选择受限应用并保存强限制规则

**Status:** completed

- [x] 今日概览显示每个受限应用的今日已用时间和剩余每日额度。
- [x] 只有应用处于前台的有效区间才计入使用时长。
- [x] 应用进入后台或手机锁屏后不继续累计前台使用时间。
- [x] 使用记录按本地日期计算，跨过本地零点后新一天从零开始。
- [x] 使用时长与每日额度边界采用一致、明确的取整规则。
- [x] 使用情况访问权限失效时显示保护失效，不展示貌似可靠的剩余时间。
- [x] 自动化测试使用可控时钟和用量 adapter 覆盖前台区间、后台区间、锁屏、零用量、额度边界和跨日场景。

## Comments

- 2026-08-21: Implemented local-day foreground usage accounting, Android UsageStats event adapter, rounded today/remaining allowance display, protection-failure handling, and automated domain/UI coverage. Unit tests, lint, and test APK compilation passed; the connected-device UI run executed 0 tests because the wireless device went offline.
