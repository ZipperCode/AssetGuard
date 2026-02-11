package com.zipper.compose.assetguard.data.repository

import com.zipper.compose.assetguard.data.local.dao.PaymentMethodDao
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

class PaymentMethodRepository(private val paymentMethodDao: PaymentMethodDao) {

    fun observeAll(): Flow<List<PaymentMethodEntity>> = paymentMethodDao.observeAll()

    suspend fun getById(id: Long): PaymentMethodEntity? = paymentMethodDao.getById(id)

    suspend fun insert(method: PaymentMethodEntity): Long = paymentMethodDao.insert(method)

    suspend fun delete(method: PaymentMethodEntity): Result<Unit> {
        if (method.isBuiltin) {
            return Result.failure(IllegalStateException("内置支付方式不可删除"))
        }
        paymentMethodDao.delete(method)
        return Result.success(Unit)
    }

    suspend fun getAll(): List<PaymentMethodEntity> = paymentMethodDao.getAll()
}
