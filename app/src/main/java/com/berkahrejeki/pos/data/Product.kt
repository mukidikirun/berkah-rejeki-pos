package com.berkahrejeki.pos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String = "",
    val name: String,
    val category: String = "Umum",
    val price: Double,
    val costPrice: Double = 0.0,
    val stock: Int = 0
)
