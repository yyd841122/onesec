package com.example.onesec

data class OneSecLaunchState(
    val title: String,
    val protectionStatus: String,
    val nextStep: String,
)

fun initialLaunchState(): OneSecLaunchState =
    OneSecLaunchState(
        title = "OneSec",
        protectionStatus = "尚未设置保护",
        nextStep = "下一步：授予权限并选择受限应用",
    )
