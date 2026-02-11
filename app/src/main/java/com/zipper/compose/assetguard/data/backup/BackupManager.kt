package com.zipper.compose.assetguard.data.backup

import android.content.Context
import android.net.Uri
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupManager(private val container: AppContainer) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val data = BackupData(
                persons = container.personRepository.getAll(),
                loans = container.loanRepository.getAll(),
                repayments = container.repaymentRepository.getAll(),
                paymentMethods = container.paymentMethodRepository.getAll()
            )
            val jsonString = json.encodeToString(BackupData.serializer(), data)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importJson(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(IllegalStateException("无法读取文件"))

            val data = json.decodeFromString(BackupData.serializer(), jsonString)

            // 先导入支付方式（因为 loan/repayment 依赖它们）
            data.paymentMethods.forEach { method ->
                container.paymentMethodDao.insert(method.copy(id = 0))
            }

            // 导入人员
            val personIdMap = mutableMapOf<Long, Long>()
            data.persons.forEach { person ->
                val oldId = person.id
                val newId = container.personDao.insert(person.copy(id = 0))
                personIdMap[oldId] = newId
            }

            // 导入借条（映射 personId）
            val loanIdMap = mutableMapOf<Long, Long>()
            data.loans.forEach { loan ->
                val oldId = loan.id
                val newPersonId = personIdMap[loan.personId] ?: return@forEach
                val newId = container.loanDao.insert(loan.copy(id = 0, personId = newPersonId))
                loanIdMap[oldId] = newId
            }

            // 导入还款（映射 loanId）
            data.repayments.forEach { repayment ->
                val newLoanId = loanIdMap[repayment.loanId] ?: return@forEach
                container.repaymentDao.insert(repayment.copy(id = 0, loanId = newLoanId))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
