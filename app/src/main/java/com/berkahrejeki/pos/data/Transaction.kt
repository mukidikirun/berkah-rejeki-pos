package com.berkahrejeki.pos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val itemsJson: String,
    val total: Double,
    val paymentMethod: String,
    val amountPaid: Double,
    val change: Double = 0.0,
    val cashierId: Long = 0,
    val cashierName: String = ""
)
