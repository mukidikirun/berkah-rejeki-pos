package com.berkahrejeki.pos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :q || '%' OR barcode = :q")
    fun search(q: String): Flow<List<Product>>

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :id")
    suspend fun reduceStock(id: Long, qty: Int)
}
