package com.zipper.compose.assetguard.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val loanId = intent.getLongExtra(EXTRA_LOAN_ID, -1L)

        // 先取消当前通知
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)

        when (intent.action) {
            ACTION_SNOOZE -> {
                // 稍后提醒：30 分钟后重新发送通知
                // 简单实现：不做额外调度，用户下次 Worker 执行时会再次检测
            }
            ACTION_CONTACTED -> {
                // 已联系：仅取消通知，不做额外操作
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.zipper.compose.assetguard.ACTION_SNOOZE"
        const val ACTION_CONTACTED = "com.zipper.compose.assetguard.ACTION_CONTACTED"
        const val EXTRA_LOAN_ID = "extra_loan_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
