package com.zipper.compose.assetguard.data.repository

import com.zipper.compose.assetguard.data.local.dao.LoanDao
import com.zipper.compose.assetguard.data.local.dao.RepaymentDao
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import kotlinx.coroutines.flow.Flow

class RepaymentRepository(
    private val repaymentDao: RepaymentDao,
    private val loanDao: LoanDao
) {

    fun observeByLoan(loanId: Long): Flow<List<RepaymentEntity>> = repaymentDao.observeByLoan(loanId)

    suspend fun getById(id: Long): RepaymentEntity? = repaymentDao.getById(id)

    suspend fun insert(repayment: RepaymentEntity): Long {
        val id = repaymentDao.insert(repayment)
        recalculateLoanStatus(repayment.loanId)
        return id
    }

    suspend fun update(repayment: RepaymentEntity) {
        repaymentDao.update(repayment.copy(updatedAt = System.currentTimeMillis()))
        recalculateLoanStatus(repayment.loanId)
    }

    suspend fun delete(repayment: RepaymentEntity): Result<Unit> = runCatching {
        repaymentDao.delete(repayment)
        recalculateLoanStatus(repayment.loanId)
    }

    suspend fun getAll(): List<RepaymentEntity> = repaymentDao.getAll()

    fun observeTotalRepaid(): Flow<Long> = repaymentDao.observeTotalRepaid()

    suspend fun getTotalRepaidForLoan(loanId: Long): Long = repaymentDao.getTotalRepaidForLoan(loanId)

    private suspend fun recalculateLoanStatus(loanId: Long) {
        val loan = loanDao.getById(loanId) ?: return
        val totalRepaid = repaymentDao.getTotalRepaidForLoan(loanId)
        val newStatus = LoanStatus.fromRepaid(totalRepaid, loan.amount)
        if (loan.status != newStatus) {
            loanDao.updateStatus(loanId, newStatus)
        }
    }
}
