package com.zipper.compose.assetguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Insert
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Delete
    suspend fun delete(person: PersonEntity)

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE id = :id")
    fun observeById(id: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM persons ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("""
        SELECT p.*,
            COALESCE(SUM(l.amount), 0) AS totalLent,
            COALESCE((SELECT SUM(r.amount) FROM repayments r INNER JOIN loans l2 ON r.loanId = l2.id WHERE l2.personId = p.id), 0) AS totalRepaid,
            COALESCE((SELECT COUNT(*) FROM loans l3 WHERE l3.personId = p.id AND l3.status NOT IN (2, 6)), 0) AS unpaidLoanCount
        FROM persons p
        LEFT JOIN loans l ON l.personId = p.id
        GROUP BY p.id
        ORDER BY (COALESCE(SUM(l.amount), 0) - COALESCE((SELECT SUM(r.amount) FROM repayments r INNER JOIN loans l2 ON r.loanId = l2.id WHERE l2.personId = p.id), 0)) DESC
    """)
    fun observeAllWithSummary(): Flow<List<PersonWithSummary>>

    @Query("""
        SELECT p.*,
            COALESCE(SUM(l.amount), 0) AS totalLent,
            COALESCE((SELECT SUM(r.amount) FROM repayments r INNER JOIN loans l2 ON r.loanId = l2.id WHERE l2.personId = p.id), 0) AS totalRepaid,
            COALESCE((SELECT COUNT(*) FROM loans l3 WHERE l3.personId = p.id AND l3.status NOT IN (2, 6)), 0) AS unpaidLoanCount
        FROM persons p
        LEFT JOIN loans l ON l.personId = p.id
        WHERE p.name LIKE '%' || :query || '%' OR p.phone LIKE '%' || :query || '%'
        GROUP BY p.id
        ORDER BY (COALESCE(SUM(l.amount), 0) - COALESCE((SELECT SUM(r.amount) FROM repayments r INNER JOIN loans l2 ON r.loanId = l2.id WHERE l2.personId = p.id), 0)) DESC
    """)
    fun searchWithSummary(query: String): Flow<List<PersonWithSummary>>

    @Query("SELECT COUNT(*) FROM loans WHERE personId = :personId AND status NOT IN (2, 6)")
    suspend fun getUnpaidLoanCount(personId: Long): Int

    @Query("SELECT * FROM persons ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT 10")
    suspend fun searchByName(query: String): List<PersonEntity>
}
