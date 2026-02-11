package com.zipper.compose.assetguard.data.repository

import com.zipper.compose.assetguard.data.local.dao.PersonDao
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import kotlinx.coroutines.flow.Flow

class PersonRepository(private val personDao: PersonDao) {

    fun observeAllWithSummary(): Flow<List<PersonWithSummary>> = personDao.observeAllWithSummary()

    fun searchWithSummary(query: String): Flow<List<PersonWithSummary>> = personDao.searchWithSummary(query)

    fun observeById(id: Long): Flow<PersonEntity?> = personDao.observeById(id)

    suspend fun getById(id: Long): PersonEntity? = personDao.getById(id)

    suspend fun insert(person: PersonEntity): Long = personDao.insert(person)

    suspend fun update(person: PersonEntity) {
        personDao.update(person.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(person: PersonEntity): Result<Unit> {
        val unpaidCount = personDao.getUnpaidLoanCount(person.id)
        if (unpaidCount > 0) {
            return Result.failure(IllegalStateException("该联系人还有 $unpaidCount 笔未结清借条，无法删除"))
        }
        personDao.delete(person)
        return Result.success(Unit)
    }

    suspend fun getAll(): List<PersonEntity> = personDao.getAll()
}
