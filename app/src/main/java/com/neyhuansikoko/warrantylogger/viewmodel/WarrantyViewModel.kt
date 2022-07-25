package com.neyhuansikoko.warrantylogger.viewmodel

import android.content.ClipData
import androidx.lifecycle.*
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.WarrantyDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WarrantyViewModel(private val warrantyDao: WarrantyDao): ViewModel() {

    val allWarranties: LiveData<List<Warranty>> = warrantyDao.getAll().asLiveData()

    fun addNewWarranty(warrantyName: String, expirationDate: Long, imageUri: String?) {
        val newWarranty = getNewWarrantyEntry(warrantyName, expirationDate, imageUri)
        insertItem(newWarranty)
    }

    private fun getNewWarrantyEntry(warrantyName: String, expirationDate: Long, imageUri: String?): Warranty {
        return Warranty(
            warrantyName = warrantyName,
            expirationDate = expirationDate,
            imageUri = imageUri
        )
    }

    private fun insertItem(newWarranty: Warranty) {
        viewModelScope.launch(Dispatchers.IO) { warrantyDao.insert(newWarranty) }
    }
}

class WarrantyViewModelFactory(private val warrantyDao: WarrantyDao) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarrantyViewModel::class.java)) {
            return WarrantyViewModel(warrantyDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}