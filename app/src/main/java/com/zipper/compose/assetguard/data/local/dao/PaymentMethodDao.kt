package com.zipper.compose.assetguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {

    @Insert
    suspend fun insert(method: PaymentMethodEntity): Long

    @Update
    suspend fun update(method: PaymentMethodEntity)

    @Delete
    suspend fun delete(method: PaymentMethodEntity)

    @Query("SELECT * FROM payment_methods ORDER BY isBuiltin DESC, createdAt ASC")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE id = :id")
    suspend fun getById(id: Long): PaymentMethodEntity?

    @Query("SELECT * FROM payment_methods ORDER BY isBuiltin DESC, createdAt ASC")
    suspend fun getAll(): List<PaymentMethodEntity>

    @Query("SELECT COUNT(*) FROM payment_methods")
    suspend fun getCount(): Int
}
