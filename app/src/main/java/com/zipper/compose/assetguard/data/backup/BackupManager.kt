package com.zipper.compose.assetguard.data.backup

import android.content.Context
import android.net.Uri
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.util.DateUtils
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

            // 导出后回读验证
            val readBack = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            }
            if (readBack != jsonString) {
                return@withContext Result.failure(IllegalStateException("导出文件验证失败"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 第一步：解析备份文件
    suspend fun parseBackup(context: Context, uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(IllegalStateException("无法读取文件"))

            val data = json.decodeFromString(BackupData.serializer(), jsonString)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 第二步：预览导入内容
    fun previewImport(data: BackupData): ImportPreview {
        val totalAmount = data.loans.sumOf { it.amount }
        val dates = data.loans.map { it.loanDate }
        val dateRange = if (dates.isNotEmpty()) {
            "${DateUtils.formatDate(dates.min())} ~ ${DateUtils.formatDate(dates.max())}"
        } else ""

        return ImportPreview(
            newPersonCount = data.persons.size,
            newLoanCount = data.loans.size,
            newRepaymentCount = data.repayments.size,
            newPaymentMethodCount = data.paymentMethods.size,
            totalAmount = totalAmount,
            dateRange = dateRange
        )
    }

    // 第三步：执行导入
    suspend fun executeImport(data: BackupData, strategy: ConflictStrategy = ConflictStrategy.SKIP): ImportResult =
        withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            var personsImported = 0
            var loansImported = 0
            var repaymentsImported = 0
            var paymentMethodsImported = 0

            try {
                // 导入支付方式
                data.paymentMethods.forEach { method ->
                    try {
                        container.paymentMethodDao.insert(method.copy(id = 0))
                        paymentMethodsImported++
                    } catch (e: Exception) {
                        if (strategy != ConflictStrategy.SKIP) {
                            errors.add("支付方式「${method.name}」导入失败: ${e.message}")
                        }
                    }
                }

                // 导入人员
                val personIdMap = mutableMapOf<Long, Long>()
                data.persons.forEach { person ->
                    try {
                        val oldId = person.id
                        val newId = container.personDao.insert(person.copy(id = 0))
                        personIdMap[oldId] = newId
                        personsImported++
                    } catch (e: Exception) {
                        errors.add("联系人「${person.name}」导入失败: ${e.message}")
                    }
                }

                // 导入借条
                val loanIdMap = mutableMapOf<Long, Long>()
                data.loans.forEach { loan ->
                    try {
                        val newPersonId = personIdMap[loan.personId]
                        if (newPersonId == null) {
                            errors.add("借条 #${loan.id} 跳过：关联联系人未导入")
                            return@forEach
                        }
                        val oldId = loan.id
                        val newId = container.loanDao.insert(loan.copy(id = 0, personId = newPersonId))
                        loanIdMap[oldId] = newId
                        loansImported++
                    } catch (e: Exception) {
                        errors.add("借条导入失败: ${e.message}")
                    }
                }

                // 导入还款
                data.repayments.forEach { repayment ->
                    try {
                        val newLoanId = loanIdMap[repayment.loanId]
                        if (newLoanId == null) {
                            errors.add("还款 #${repayment.id} 跳过：关联借条未导入")
                            return@forEach
                        }
                        container.repaymentDao.insert(repayment.copy(id = 0, loanId = newLoanId))
                        repaymentsImported++
                    } catch (e: Exception) {
                        errors.add("还款导入失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                errors.add("导入过程异常: ${e.message}")
            }

            ImportResult(
                personsImported = personsImported,
                loansImported = loansImported,
                repaymentsImported = repaymentsImported,
                paymentMethodsImported = paymentMethodsImported,
                errors = errors
            )
        }

    // 兼容旧接口
    suspend fun importJson(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val parseResult = parseBackup(context, uri)
        if (parseResult.isFailure) {
            return@withContext Result.failure(parseResult.exceptionOrNull()!!)
        }
        val data = parseResult.getOrThrow()
        val importResult = executeImport(data)
        if (importResult.isSuccess) Result.success(Unit)
        else Result.failure(IllegalStateException(importResult.errors.joinToString("\n")))
    }

    // 部分导出（用于批量操作）
    suspend fun exportPartial(context: Context, uri: Uri, personIds: Set<Long>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val persons = container.personRepository.getAll().filter { it.id in personIds }
                val loans = container.loanRepository.getAll().filter { it.personId in personIds }
                val loanIds = loans.map { it.id }.toSet()
                val repayments = container.repaymentRepository.getAll().filter { it.loanId in loanIds }
                val paymentMethods = container.paymentMethodRepository.getAll()

                val data = BackupData(
                    persons = persons,
                    loans = loans,
                    repayments = repayments,
                    paymentMethods = paymentMethods
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
}
