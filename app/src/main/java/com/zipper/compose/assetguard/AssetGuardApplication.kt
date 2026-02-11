package com.zipper.compose.assetguard

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.notification.NotificationHelper
import com.zipper.compose.assetguard.notification.ReminderWorker
import java.util.concurrent.TimeUnit

class AssetGuardApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        NotificationHelper.createNotificationChannel(this)
        scheduleReminderWork()
    }

    private fun scheduleReminderWork() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "loan_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
