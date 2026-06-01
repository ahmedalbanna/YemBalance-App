package com.example.data.local

import androidx.room.*
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getTransactionsForEmployeeFlow(employeeId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE employeeId = :employeeId AND shiftDate = :shiftDate ORDER BY timestamp DESC")
    fun getTransactionsForEmployeeTodayFlow(employeeId: Int, shiftDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE shiftDate = :shiftDate ORDER BY timestamp DESC")
    fun getTransactionsForDateFlow(shiftDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("UPDATE transactions SET isCleared = 1, clearedAt = :clearedAt WHERE employeeId = :employeeId AND shiftDate = :shiftDate")
    suspend fun clearTransactionsForEmployeeDate(employeeId: Int, shiftDate: String, clearedAt: Long)

    @Query("SELECT DISTINCT shiftDate FROM transactions ORDER BY shiftDate DESC")
    fun getAllUniqueShiftDatesFlow(): Flow<List<String>>

    // Simple raw query results for non-flow triggers, e.g. calculations during settlement
    @Query("SELECT * FROM transactions WHERE employeeId = :employeeId AND shiftDate = :shiftDate")
    suspend fun getTransactionsForEmployeeDateDirect(employeeId: Int, shiftDate: String): List<TransactionEntity>
}
