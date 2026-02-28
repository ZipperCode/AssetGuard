package com.zipper.compose.assetguard.data.preferences

import com.zipper.compose.assetguard.data.model.ThemeMode

data class UserPreferences(
    // 提醒设置
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderAdvanceDays: Int = 1,
    val onlyOverdue: Boolean = false,
    val silentStartHour: Int = 22,
    val silentEndHour: Int = 8,

    // 通知权限
    val notificationPermissionAsked: Boolean = false,

    // 安全设置（预留）
    val appLockEnabled: Boolean = false,

    // 外观设置
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
