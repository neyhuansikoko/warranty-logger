package com.neyhuansikoko.warrantylogger.repository

import com.neyhuansikoko.warrantylogger.database.dao.ImageDao
import com.neyhuansikoko.warrantylogger.database.dao.WarrantyDao
import com.neyhuansikoko.warrantylogger.database.model.Image
import com.neyhuansikoko.warrantylogger.database.model.Warranty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WarrantyRepository @Inject constructor(
    private val warrantyDao: WarrantyDao,
    private val imageDao: ImageDao
) {
    fun getAllWarranties() = warrantyDao.getAllWarranties()

    suspend fun insertWarranty(warranty: Warranty) = withContext(Dispatchers.IO) {
        warrantyDao.insertWarranty(warranty)
    }

    suspend fun updateWarranty(warranty: Warranty) {
        withContext(Dispatchers.IO) {
            warrantyDao.updateWarranty(warranty)
        }
    }

    suspend fun deleteWarranty(warranty: Warranty) {
        withContext(Dispatchers.IO) {
            warrantyDao.deleteWarranty(warranty)
        }
    }

    suspend fun deleteSelectedWarranties(warrantyIdList: List<Int>) {
        withContext(Dispatchers.IO) {
            warrantyDao.deleteSelectedWarranties(warrantyIdList)
        }
    }

    suspend fun getFlowOfAllImagesForWarranty(warrantyId: Int) = withContext(Dispatchers.IO) {
        imageDao.getFlowOfAllImagesForWarranty(warrantyId)
    }

    suspend fun getAllImagesForWarranty(warrantyId: Int) = withContext(Dispatchers.IO) {
        imageDao.getAllImagesForWarranty(warrantyId)
    }

    suspend fun insertAllImages(vararg image: Image) {
        withContext(Dispatchers.IO) {
            imageDao.insertAllImages(*image)
        }
    }

    suspend fun deleteSelectedImages(vararg image: Image) {
        withContext(Dispatchers.IO) {
            imageDao.deleteSelectedImages(*image)
        }
    }
}