package com.neyhuansikoko.warrantylogger.database.dao

import androidx.room.*
import com.neyhuansikoko.warrantylogger.database.model.Warranty
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarranty(warranty: Warranty): Long

    @Update
    suspend fun updateWarranty(warranty: Warranty)

    @Delete
    suspend fun deleteWarranty(warranty: Warranty)

    @Query("DELETE FROM warranties WHERE warranty_id IN (:list)")
    suspend fun deleteSelectedWarranties(list: List<Int>)

    @Query("SELECT * FROM warranties ORDER BY expiration_date ASC")
    fun getAllWarranties(): Flow<List<Warranty>>
}