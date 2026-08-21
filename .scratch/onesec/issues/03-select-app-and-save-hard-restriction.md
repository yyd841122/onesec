# 03: 选择受限应用并保存强限制规则

**What to build:** 让使用者从设备上的已安装应用中选择一个受限应用，为它设置每日额度和强限制，并在 OneSec 重启后继续看到同一条规则。

**Blocked by:** 01: 创建可运行的 OneSec Android 外壳

**Status:** completed

- [x] 使用者可以打开已安装应用列表，并看到应用名称和图标。
- [x] OneSec 本身以及不适合管理的系统入口不会被误选为普通受限应用。
- [x] 使用者可以选择一个应用并创建强限制。
- [x] 新强限制的默认每日额度为30分钟，且可以在保存前调整。
- [x] 规则以稳定的应用标识保存，而不是仅依赖易变化的显示名称。
- [x] 关闭并重新启动 OneSec 后，受限应用、限制等级和每日额度保持不变。
- [x] 自动化测试通过应用目录与持久化 adapters 验证选择、保存和恢复行为，不依赖真实设备应用列表。

## Comments

- 2026-08-21: Implemented filtered installed-app catalog with icons, hard-restriction editing, package-name persistence, restart restoration, and fake-adapter unit/UI coverage. Three Compose instrumentation tests passed on PCAM00.
