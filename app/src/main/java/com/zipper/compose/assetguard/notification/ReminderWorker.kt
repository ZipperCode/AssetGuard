package com.zipper.compose.assetguard.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zipper.compose.assetguard.AssetGuardApplication
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AssetGuardApplication ?: return Result.failure()
        val loanRepository = app.container.loanRepository
        val personDao = app.container.personDao
        val prefsRepo = app.container.userPreferencesRepository

        // 读取用户偏好
        val prefs = prefsRepo.userPreferences.first()

        // 检查静音时段
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (isInSilentHours(currentHour, prefs.silentStartHour, prefs.silentEndHour)) {
            return Result.success()
        }

        // 根据偏好计算阈值日期
        val thresholdDate = if (prefs.onlyOverdue) {
            DateUtils.todayStart()
        } else {
            DateUtils.daysFromNow(prefs.reminderAdvanceDays)
        }

        val dueSoonLoans = loanRepository.getOverdueOrDueSoon(thresholdDate)
        if (dueSoonLoans.isEmpty()) return Result.success()

        NotificationHelper.createNotificationChannel(applicationContext)

        // 按人员分组
        val grouped = dueSoonLoans.groupBy { it.personId }

        for ((personId, loans) in grouped) {
            val person = personDao.getById(personId) ?: continue
            val totalAmount = loans.sumOf { it.amount }
            val overdueCount = loans.count { loan ->
                loan.dueDate != null && DateUtils.daysUntilDue(loan.dueDate) < 0
            }

            val title = if (overdueCount > 0) {
                "${person.name} 有 ${loans.size} 笔借条需要关注"
            } else {
                "${person.name} 有借条即将到期"
            }

            val message = "共 ${MoneyUtils.formatCents(totalAmount)}，" +
                    if (overdueCount > 0) "其中 $overdueCount 笔已过期"
                    else "${loans.size} 笔即将到期"

            // 如果只有一笔，携带 loanId 以支持 DeepLink
            val singleLoanId = if (loans.size == 1) loans.first().id else null

            NotificationHelper.showReminderNotification(
                context = applicationContext,
                title = title,
                message = message,
                notificationId = personId.toInt(),
                loanId = singleLoanId
            )
        }

        return Result.success()
    }

    private fun isInSilentHours(currentHour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour < endHour) {
            currentHour in startHour until endHour
        } else {
            currentHour >= startHour || currentHour < endHour
        }
    }
}
