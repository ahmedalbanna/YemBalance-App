package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val employeeName: String,
    val dateString: String, // YYYY-MM-DD
    val totalIncome: Double,
    val totalExpenseShop: Double,
    val totalExpensePersonal: Double,
    val calculatedSalary: Double,
    val amountPaid: Double,
    val timestamp: Long = System.currentTimeMillis()
)
