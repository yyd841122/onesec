package com.example.onesec

import org.junit.Assert.assertEquals
import org.junit.Test

class OneSecLaunchStateTest {
    @Test
    fun `launch state identifies OneSec and explains that protection is not configured`() {
        val state = initialLaunchState()

        assertEquals("OneSec", state.title)
        assertEquals("尚未设置保护", state.configurationStatus)
        assertEquals("下一步：授予权限并选择受限应用", state.nextStep)
    }
}
