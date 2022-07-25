package com.neyhuansikoko.warrantylogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.neyhuansikoko.warrantylogger.database.WarrantyDao

class WarrantyViewModel(warrantyDao: WarrantyDao): ViewModel() {
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