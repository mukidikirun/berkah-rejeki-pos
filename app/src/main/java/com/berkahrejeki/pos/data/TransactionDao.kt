package com.berkahrejeki.pos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    suspend fun getByRangeOnce(from: Long, to: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE cashierId = :uid AND timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    suspend fun getByCashier(uid: Long, from: Long, to: Long): List<Transaction>

    @Insert
    suspend fun insert(t: Transaction): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countAll(): Int
}
