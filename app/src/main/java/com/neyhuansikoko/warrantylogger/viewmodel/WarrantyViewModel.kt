package com.neyhuansikoko.warrantylogger.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.work.*
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.deleteImageFile
import com.neyhuansikoko.warrantylogger.database.isValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class WarrantyViewModel(application: Application): AndroidViewModel(application) {

    enum class WarrantyAttribute { NAME, DATE }
    enum class WarrantySort { ASC, DSC }

    private val warrantyDao = getApplication<WarrantyLoggerApplication>().database.warrantyDao()

    val displayModel: MutableLiveData<Warranty> = MutableLiveData(DEFAULT_MODEL)
    var inputModel: Warranty = DEFAULT_MODEL

    val allWarranties: LiveData<List<Warranty>> = warrantyDao.getAll().asLiveData()
    val filterWarranties: MutableLiveData<List<Warranty>> = MutableLiveData()
    val mediatorWarranties: MediatorLiveData<List<Warranty>> = MediatorLiveData()
    val mediatorSize: Int get() = mediatorWarranties.value?.size ?: 0

    var tempImage: File? = null

    val deleteList: MutableLiveData<MutableList<Warranty>> = MutableLiveData(mutableListOf())
    val deleteSize: Int get() = deleteList.value?.size ?: 0

    private val workManager = WorkManager.getInstance(getApplication())

    init {
        mediatorWarranties.addSource(allWarranties) {
            mediatorWarranties.value = it
        }
        mediatorWarranties.addSource(filterWarranties) {
            mediatorWarranties.value = it
        }
    }

    fun getSortedWarranties(list: List<Warranty>, attribute: WarrantyAttribute, sort: WarrantySort): List<Warranty> {
        return if (attribute == WarrantyAttribute.NAME) {
            if (sort == WarrantySort.ASC) {
                list.sortedBy { it.warrantyName }
            } else {
                list.sortedByDescending { it.warrantyName }
            }
        } else  {
            if (sort == WarrantySort.ASC) {
                list.sortedBy { it.expirationDate }
            } else {
                list.sortedByDescending { it.expirationDate }
            }
        }
    }

    fun getFilteredWarranties(query: String?): List<Warranty> {
        return allWarranties.value?.filter { it.warrantyName.contains(query ?: "", true) } ?: listOf()
    }

    fun assignModel(warranty: Warranty) {
        displayModel.value = warranty
        inputModel = warranty.copy()
    }

    fun resetModel() {
        displayModel.value = DEFAULT_MODEL
        inputModel = DEFAULT_MODEL
    }

    fun addAllToDelete() {
        deleteList.value = allWarranties.value?.toMutableList()
    }

    fun clearDelete() {
        deleteList.apply {
            value?.clear()
            notifyObserver()
        }
    }

    fun addDelete(warranty: Warranty) {
        deleteList.apply {
            value?.add(warranty)
            deleteList.notifyObserver()
        }
    }

    fun removeDelete(warranty: Warranty) {
        deleteList.apply {
            value?.remove(warranty)
            deleteList.notifyObserver()
        }
    }

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

    fun deleteSelectedWarranty() {
        deleteList.value?.let { deleteList ->
            if (deleteList.isNotEmpty()) {
                deleteList.forEach {
                    it.deleteImageFile(getApplication())
                }
                val list: List<Int> = deleteList.map { it.id }

                viewModelScope.launch(Dispatchers.IO) {
                    warrantyDao.deleteSelected(list)
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
            tempImage = null
        }
        return newImage
    }

    fun clearTempImage() {
        tempImage = null
        clearCache(getApplication())
    }

    internal fun scheduleReminder(warranty: Warranty, duration: String , timeUnit: String) : Long {
        val data = Data.Builder().putString(ExpirationNotifierWorker.nameKey, warranty.warrantyName).build()

        val delayTime = getDaysToDate(warranty.expirationDate) - inputToDays(duration.toLong(), timeUnit)

        if (delayTime > 0) {
            val request = OneTimeWorkRequestBuilder<ExpirationNotifierWorker>()
                .setInitialDelay(delayTime, TimeUnit.DAYS)
                .setInputData(data)
                .build()

            //Enqueue unique work with warranty's id as unique identifier
            workManager.enqueueUniqueWork(
                warranty.id.toString(),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        return delayTime
    }

    internal suspend fun doesWorkExist(): Boolean {
        displayModel.value?.takeIf { it.isValid() }?.let {
            val workInfo = workManager.getWorkInfosForUniqueWork(it.id.toString()).await()

            if (workInfo.size == 1) {
                val workState = workInfo[0].state

                return (
                        workState == WorkInfo.State.BLOCKED ||
                                workState == WorkInfo.State.ENQUEUED ||
                                workState == WorkInfo.State.RUNNING
                        )
            }
        }

        return false
    }

    internal fun cancelWork() {
        displayModel.value?.takeIf { it.isValid() }?.let { workManager.cancelUniqueWork(it.id.toString()) }
    }

    fun calculateExpirationDate(duration: String, timeUnit: String): Long {
        inputModel.apply {
            return inputModel.purchaseDate + inputToDays(duration.toLong(), timeUnit) * DAY_MILLIS
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearCache(getApplication())
    }
}