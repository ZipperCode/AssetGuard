package com.zipper.compose.assetguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)

    @Delete
    suspend fun delete(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getById(id: Long): LoanEntity?

    @Transaction
    @Query("SELECT * FROM loans WHERE id = :id")
    fun observeWithRepayments(id: Long): Flow<LoanWithRepayments?>

    @Transaction
    @Query("SELECT * FROM loans WHERE personId = :personId ORDER BY loanDate DESC")
    fun observeByPerson(personId: Long): Flow<List<LoanWithRepayments>>

    @Query("SELECT * FROM loans WHERE personId = :personId ORDER BY loanDate DESC")
    fun observeLoansByPerson(personId: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE personId = :personId ORDER BY loanDate DESC")
    suspend fun getByPersonId(personId: Long): List<LoanEntity>

    @Query("UPDATE loans SET status = :status, updatedAt = :updatedAt WHERE id = :loanId")
    suspend fun updateStatus(loanId: Long, status: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        SELECT * FROM loans
        WHERE dueDate IS NOT NULL AND status NOT IN (2, 6)
        AND dueDate <= :thresholdDate
        ORDER BY dueDate ASC
    """)
    suspend fun getOverdueOrDueSoon(thresholdDate: Long): List<LoanEntity>

    @Query("""
        SELECT * FROM loans
        WHERE dueDate IS NOT NULL AND status NOT IN (2, 6)
        AND dueDate <= :thresholdDate
        ORDER BY dueDate ASC
    """)
    fun observeOverdueOrDueSoon(thresholdDate: Long): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    suspend fun getAll(): List<LoanEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM loans")
    fun observeTotalLent(): Flow<Long>

    @Query("SELECT COUNT(*) FROM loans WHERE status NOT IN (2, 6)")
    fun observeUnpaidCount(): Flow<Int>

    // KPI 查询
    @Query("""
        SELECT COUNT(*) FROM loans
        WHERE dueDate IS NOT NULL AND status NOT IN (2, 6)
        AND dueDate < :todayStart
    """)
    fun observeOverdueCount(todayStart: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM loans
        WHERE dueDate IS NOT NULL AND status NOT IN (2, 6)
        AND dueDate >= :todayStart AND dueDate < :tomorrowStart
    """)
    fun observeDueTodayCount(todayStart: Long, tomorrowStart: Long): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(l.amount), 0) - COALESCE(
            (SELECT SUM(r.amount) FROM repayments r WHERE r.loanId IN (SELECT id FROM loans WHERE status NOT IN (2, 6))),
            0
        ) FROM loans l WHERE l.status NOT IN (2, 6)
    """)
    fun observeTotalOutstanding(): Flow<Long>

    // 搜索筛选查询
    @Query("""
        SELECT * FROM loans
        WHERE status NOT IN (2, 6)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:dueBefore IS NULL OR (dueDate IS NOT NULL AND dueDate <= :dueBefore))
        AND (:dueAfter IS NULL OR (dueDate IS NOT NULL AND dueDate >= :dueAfter))
        ORDER BY createdAt DESC
    """)
    fun searchLoans(
        minAmount: Long? = null,
        maxAmount: Long? = null,
        statusFilter: Int? = null,
        dueBefore: Long? = null,
        dueAfter: Long? = null
    ): Flow<List<LoanEntity>>
}
