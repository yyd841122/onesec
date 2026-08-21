package com.example.onesec

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class OneSecAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
