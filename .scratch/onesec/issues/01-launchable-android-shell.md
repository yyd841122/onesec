# 01: 创建可运行的 OneSec Android 外壳

**What to build:** 创建一个最小但完整的 OneSec Android 应用，使开发者可以构建、测试、安装并在目标手机上启动它。应用启动后应展示 OneSec 身份和当前为初始状态的页面，为后续纵向功能提供可持续演进的工程基线。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 工程使用 Kotlin、Jetpack Compose 与 Material 3，并支持 Android 10 及以上系统。
- [ ] Debug 构建可以通过命令行成功完成。
- [ ] 基础自动化测试可以通过命令行运行并成功完成。
- [ ] 应用可通过 ADB 安装到 OPPO Reno PCAM00，并能正常启动。
- [ ] 启动页显示 OneSec 名称和明确的初始状态，不包含未实现功能的虚假数据。
- [ ] 应用清单不申请联网权限。
- [ ] 工程结构为后续限制决策模块和 Android adapters 留出清晰位置，但不提前创建没有第二个实现的假设 seam。

## Comments

