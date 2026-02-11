package com.zipper.compose.assetguard.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zipper.compose.assetguard.AssetGuardApplication
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AssetGuardApplication ?: return Result.failure()
        val loanRepository = app.container.loanRepository
        val personDao = app.container.personDao

        // 查找明天之前到期的未还借条
        val threshold = DateUtils.daysFromNow(1)
        val dueSoonLoans = loanRepository.getOverdueOrDueSoon(threshold)

        if (dueSoonLoans.isEmpty()) return Result.success()

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

            NotificationHelper.showReminderNotification(
                applicationContext,
                title = title,
                message = message,
                notificationId = personId.toInt()
            )
        }

        return Result.success()
    }
}
