package com.berkahrejeki.pos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY role, username")
    fun getAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :u AND active = 1 LIMIT 1")
    suspend fun findByUsername(u: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}
