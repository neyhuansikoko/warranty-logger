package com.neyhuansikoko.warrantylogger.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(warranty: Warranty)

    @Update
    suspend fun update(warranty: Warranty)

    @Delete
    suspend fun delete(warranty: Warranty)

    @Query("SELECT * FROM warranty ORDER BY expiration_date ASC")
    fun getAll(): Flow<List<Warranty>>

    @Query("SELECT * FROM warranty WHERE id = :id")
    suspend fun getById(id: Int): Warranty
}