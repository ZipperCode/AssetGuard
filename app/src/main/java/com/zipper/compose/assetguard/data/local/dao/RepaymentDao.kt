package com.zipper.compose.assetguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepaymentDao {

    @Insert
    suspend fun insert(repayment: RepaymentEntity): Long

    @Update
    suspend fun update(repayment: RepaymentEntity)

    @Delete
    suspend fun delete(repayment: RepaymentEntity)

    @Query("SELECT * FROM repayments WHERE id = :id")
    suspend fun getById(id: Long): RepaymentEntity?

    @Query("SELECT * FROM repayments WHERE loanId = :loanId ORDER BY repayDate DESC")
    fun observeByLoan(loanId: Long): Flow<List<RepaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM repayments WHERE loanId = :loanId")
    suspend fun getTotalRepaidForLoan(loanId: Long): Long

    @Query("SELECT * FROM repayments ORDER BY createdAt DESC")
    suspend fun getAll(): List<RepaymentEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM repayments")
    fun observeTotalRepaid(): Flow<Long>
}
