package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phone: String,
    val name: String,
    val role: String, // "MANAGER" or "EMPLOYEE"
    val password: String,
    val baseSalary: Double, // Daily base salary in Yemeni Rial
    val commissionPercent: Double, // Percentage from his daily income
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
