package com.example.onesec

data class OneSecLaunchState(
    val title: String,
    val configurationStatus: String,
    val nextStep: String,
)

fun initialLaunchState(): OneSecLaunchState =
    OneSecLaunchState(
        title = "OneSec",
        configurationStatus = "尚未设置保护",
        nextStep = "下一步：授予权限并选择受限应用",
    )
