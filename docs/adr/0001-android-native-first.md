# 第一版采用 Android 原生实现

OneSec 第一版只服务 Android，并采用 Kotlin 与 Jetpack Compose，而不使用跨平台框架。应用限制依赖 UsageStatsManager、AccessibilityService 和后台运行机制等平台能力；直接使用 Android 原生接口能减少跨平台桥接的不确定性，并优先保证 OPPO Reno 测试机上的可靠性，代价是未来支持 iPhone 时需要单独实现。
