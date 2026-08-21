# 06: 重启后恢复保护并适配 ColorOS 后台运行

**What to build:** 让 OneSec 在 OPPO Reno 重启后恢复已保存的保护规则，并通过安静的持续通知和设备设置指引提高 ColorOS 11.1 下的后台运行可靠性。

**Blocked by:** 05: 额度耗尽后执行强限制拦截

**Status:** completed

- [x] 手机重启后，OneSec 能恢复监控并继续执行已保存的强限制。
- [x] 重启不会清除当日用量、规则或保护状态。
- [x] 监控运行时展示低干扰的持续通知，通知内容准确说明 OneSec 正在保护。
- [x] 使用者可以看到适用于 OPPO/ColorOS 的后台运行和电池优化设置指引。
- [x] 如果系统阻止监控恢复，OneSec 下次打开时明确显示保护失效或需要修复的状态。
- [x] 自动化测试覆盖启动恢复决定和持久状态恢复。
- [x] 在 OPPO Reno PCAM00 上完成一次物理重启验证，并记录 ColorOS 设置与观察结果。

## Comments

- 2026-08-21: Implemented boot recovery, persisted recovery health, a quiet ongoing protection notification, and OPPO/ColorOS 11.1 background/battery guidance. Automated unit tests, Android-test compilation, and lint pass. No adb device was available, so physical restart verification remains pending. On PCAM00, enable OneSec auto-start, associated start, and background activity; disable battery optimization; reboot; then verify the notification returns, saved hard restrictions still intervene, today's exhausted state remains, and reopening OneSec reports either available protection or a repair state.
- 2026-08-21: Physical reboot verification passed on OPPO Reno PCAM00 / ColorOS 11.1: after restarting the phone, the saved restricted app remained under its strong restriction and could not be opened. The ColorOS battery-optimization list did not show OneSec through the original generic settings entry; a follow-up code fix now requests the OneSec-specific exemption directly, but could not yet be rebuilt and installed because the execution environment blocked Gradle/ADB local sockets.
- 2026-08-21: Verified the follow-up battery-optimization fix on PCAM00 / ColorOS 11.1. The OneSec-specific exemption prompt opens correctly and was confirmed by the user.
