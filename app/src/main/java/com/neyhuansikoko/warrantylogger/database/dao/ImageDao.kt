package com.neyhuansikoko.warrantylogger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.neyhuansikoko.warrantylogger.database.model.Image
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {

    @Insert
    suspend fun insertAllImages(vararg image: Image)

    @Delete
    suspend fun deleteSelectedImages(vararg image: Image)

    @Query("SELECT * FROM images WHERE warranty_id = :warrantyId ORDER BY created_date ASC")
    fun getFlowOfAllImagesForWarranty(warrantyId: Int): Flow<List<Image>>

    @Query("SELECT * FROM images WHERE warranty_id = :warrantyId ORDER BY created_date ASC")
    fun getAllImagesForWarranty(warrantyId: Int): List<Image>
}