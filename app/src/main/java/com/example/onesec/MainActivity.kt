package com.example.onesec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = PermissionGuidanceController(AndroidPermissionGateway(this))
        setContent {
            PermissionGuidanceRoute(controller)
        }
    }
}
