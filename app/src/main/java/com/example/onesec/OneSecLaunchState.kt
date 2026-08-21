package com.example.onesec

data class OneSecLaunchState(
    val title: String,
    val status: String,
    val nextStep: String,
)

fun initialLaunchState(): OneSecLaunchState =
    OneSecLaunchState(
        title = "OneSec",
        status = "尚未设置保护",
        nextStep = "下一步：授予权限并选择要限制的应用",
    )

