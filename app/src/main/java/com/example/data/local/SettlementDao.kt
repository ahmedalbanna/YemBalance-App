package com.example.data.local

import androidx.room.*
import com.example.data.model.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements ORDER BY timestamp DESC")
    fun getAllSettlementsFlow(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getSettlementsForEmployeeFlow(employeeId: Int): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE employeeId = :employeeId AND dateString = :dateString LIMIT 1")
    suspend fun getSettlementForEmployeeAndDate(employeeId: Int, dateString: String): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteSettlementById(id: Int)
}
