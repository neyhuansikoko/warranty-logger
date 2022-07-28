package com.neyhuansikoko.warrantylogger.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.neyhuansikoko.warrantylogger.DEFAULT_MODEL
import com.neyhuansikoko.warrantylogger.database.*
import com.neyhuansikoko.warrantylogger.getImageFile
import com.neyhuansikoko.warrantylogger.getImageFileAbs
import com.neyhuansikoko.warrantylogger.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class WarrantyViewModel(private val warrantyDao: WarrantyDao): ViewModel() {

    val modelWarranty: MutableLiveData<Warranty> = MutableLiveData(DEFAULT_MODEL)
    var modelWarrantySignature: Int = 0

    val allWarranties: LiveData<List<Warranty>> = warrantyDao.getAll().asLiveData()
    var tempImage: File? = null

    fun resetModel() {
        modelWarranty.value?.let { model ->
            if (model.isValid()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val defaultModel = warrantyDao.getById(model.id)
                    withContext(Dispatchers.Main) {
                        modelWarranty.value = defaultModel
                    }
                }
            } else {
                modelWarranty.value = DEFAULT_MODEL
            }
        }
    }

    fun insertWarranty(context: Context) {
        modelWarranty.value?.let { model ->
            viewModelScope.launch(Dispatchers.IO) {
                runBlocking {
                    saveTempImage(context)?.let { newImage ->
                        model.image = newImage.name
                    }

                    warrantyDao.insert(model)
                }
            }
        }
    }

    fun updateWarranty(context: Context) {
        modelWarranty.value?.let { model ->
            if (model.isValid()) {
                viewModelScope.launch(Dispatchers.IO) {
                    //Needed to run instructions in order
                    runBlocking {
                        saveTempImage(context)?.let { newImage ->
                            model.deleteImageFile(context)
                            model.image = newImage.name
                        }

                        warrantyDao.update(model)

                        withContext(Dispatchers.Main) {
                            modelWarrantySignature = model.hashCode()
                        }
                    }
                }
            }
        }
    }

    fun deleteWarranty(context: Context) {
        modelWarranty.value?.let { model ->
            if (model.isValid()) {
                viewModelScope.launch(Dispatchers.IO) {
                    model.deleteImageFile(context)

                    warrantyDao.delete(model)
                    withContext(Dispatchers.Main) {
                        modelWarranty.value = DEFAULT_MODEL
                    }
                }
            }
        }
    }

    //Copy temp image to non-cache directory
     private fun saveTempImage(context: Context): File? {
        var newImage: File? = null
        tempImage?.let { temp ->
            newImage = getImageFileAbs(context.applicationContext, temp.name)
            temp.copyTo(newImage!!)
            temp.delete()
        }
        return newImage
    }

    fun clearTempImage() {
        tempImage?.let {
            it.delete()
            tempImage = null
        }
    }

    //TODO: Remove
//    fun testInsertTwentyWarranty() {
//        for (i in 1..20) {
//            val date = Calendar.getInstance()
//            date.add(Calendar.MONTH, i)
//            addNewWarranty("TestObject #$i", date.timeInMillis, null)
//        }
//    }

    override fun onCleared() {
        super.onCleared()
        clearTempImage()
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