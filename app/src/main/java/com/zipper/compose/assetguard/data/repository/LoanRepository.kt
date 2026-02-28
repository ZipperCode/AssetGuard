package com.zipper.compose.assetguard.data.repository

import com.zipper.compose.assetguard.data.local.dao.LoanDao
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import kotlinx.coroutines.flow.Flow

class LoanRepository(private val loanDao: LoanDao) {

    suspend fun insert(loan: LoanEntity): Long = loanDao.insert(loan)

    suspend fun update(loan: LoanEntity) {
        loanDao.update(loan.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(loan: LoanEntity): Result<Unit> = runCatching {
        loanDao.delete(loan)
    }

    suspend fun getById(id: Long): LoanEntity? = loanDao.getById(id)

    fun observeWithRepayments(loanId: Long): Flow<LoanWithRepayments?> = loanDao.observeWithRepayments(loanId)

    fun observeByPerson(personId: Long): Flow<List<LoanWithRepayments>> = loanDao.observeByPerson(personId)

    fun observeLoansByPerson(personId: Long): Flow<List<LoanEntity>> = loanDao.observeLoansByPerson(personId)

    suspend fun getByPersonId(personId: Long): List<LoanEntity> = loanDao.getByPersonId(personId)

    suspend fun updateStatus(loanId: Long, status: Int) = loanDao.updateStatus(loanId, status)

    suspend fun getOverdueOrDueSoon(thresholdDate: Long): List<LoanEntity> = loanDao.getOverdueOrDueSoon(thresholdDate)

    fun observeOverdueOrDueSoon(thresholdDate: Long): Flow<List<LoanEntity>> = loanDao.observeOverdueOrDueSoon(thresholdDate)

    suspend fun getAll(): List<LoanEntity> = loanDao.getAll()

    fun observeTotalLent(): Flow<Long> = loanDao.observeTotalLent()

    fun observeUnpaidCount(): Flow<Int> = loanDao.observeUnpaidCount()

    // KPI
    fun observeOverdueCount(todayStart: Long): Flow<Int> = loanDao.observeOverdueCount(todayStart)

    fun observeDueTodayCount(todayStart: Long, tomorrowStart: Long): Flow<Int> =
        loanDao.observeDueTodayCount(todayStart, tomorrowStart)

    fun observeTotalOutstanding(): Flow<Long> = loanDao.observeTotalOutstanding()

    // 搜索
    fun searchLoans(
        minAmount: Long? = null,
        maxAmount: Long? = null,
        statusFilter: Int? = null,
        dueBefore: Long? = null,
        dueAfter: Long? = null
    ): Flow<List<LoanEntity>> = loanDao.searchLoans(minAmount, maxAmount, statusFilter, dueBefore, dueAfter)
}
