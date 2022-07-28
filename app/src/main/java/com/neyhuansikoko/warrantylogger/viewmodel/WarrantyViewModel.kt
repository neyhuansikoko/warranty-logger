package com.neyhuansikoko.warrantylogger.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.neyhuansikoko.warrantylogger.DEFAULT_MODEL
import com.neyhuansikoko.warrantylogger.WarrantyLoggerApplication
import com.neyhuansikoko.warrantylogger.clearCache
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.deleteImageFile
import com.neyhuansikoko.warrantylogger.database.isValid
import com.neyhuansikoko.warrantylogger.getImageFileAbs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WarrantyViewModel(application: Application): AndroidViewModel(application) {

    private val warrantyDao = getApplication<WarrantyLoggerApplication>().database.warrantyDao()

    val displayModel: MutableLiveData<Warranty> = MutableLiveData(DEFAULT_MODEL)
    var inputModel: Warranty = DEFAULT_MODEL

    val allWarranties: LiveData<List<Warranty>> = warrantyDao.getAll().asLiveData()
    var tempImage: File? = null

    fun insertWarranty() {
        if (!inputModel.isValid()) {
            saveTempImage()?.let { newImage ->
                inputModel.image = newImage.name
            }
            viewModelScope.launch(Dispatchers.IO) { warrantyDao.insert(inputModel) }
        }
    }

    fun updateWarranty() {
        if (inputModel.isValid()) {
            viewModelScope.launch(Dispatchers.IO) {
                saveTempImage()?.let { newImage ->
                    inputModel.deleteImageFile(getApplication())
                    inputModel.image = newImage.name
                }
                warrantyDao.update(inputModel)

                withContext(Dispatchers.Main) {
                    displayModel.value = inputModel.copy()
                }
            }
        }
    }

    fun deleteWarranty() {
        if (inputModel.isValid()) {
            inputModel.deleteImageFile(getApplication())
            viewModelScope.launch(Dispatchers.IO) {
                warrantyDao.delete(inputModel)

                withContext(Dispatchers.Main) {
                    inputModel = DEFAULT_MODEL
                }
            }
        }
    }

    //Copy temp image to non-cache directory
     private fun saveTempImage(): File? {
        var newImage: File? = null
        tempImage?.let { temp ->
            newImage = getImageFileAbs(getApplication(), temp.name)
            temp.copyTo(newImage!!)
            temp.delete()
        }
        return newImage
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
        clearCache(getApplication())
    }
}