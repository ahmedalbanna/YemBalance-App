package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val employeeName: String,
    val type: String, // "INCOME" or "EXPENSE"
    val expenseCategory: String? = null, // "SHOP" (shop supplies) or "PERSONAL" (lunch, personal spending)
    val amount: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isCleared: Boolean = false, // Manager settlement status
    val clearedAt: Long? = null,
    val shiftDate: String // YYYY-MM-DD
)
