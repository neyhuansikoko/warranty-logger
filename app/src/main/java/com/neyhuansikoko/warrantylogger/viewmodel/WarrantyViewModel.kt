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
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
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

    private val _tempImage: MutableLiveData<File?> = MutableLiveData()
    val tempImage: LiveData<File?> get() = _tempImage

    private val _workExist: MutableLiveData<Boolean> = MutableLiveData()
    val workExist: LiveData<Boolean> get() = _workExist

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
        tempImage.value?.let { temp ->
            newImage = getImageFileAbs(getApplication(), temp.name)
            temp.copyTo(newImage!!)
            temp.delete()
            _tempImage.value = null
        }
        return newImage
    }

    fun clearTempImage() {
        _tempImage.value = null
        clearCache(getApplication())
    }

    internal fun scheduleReminder(warranty: Warranty, duration: String , timeUnit: String) : Long {
        val data = Data.Builder().putString(ExpirationNotifierWorker.nameKey, warranty.warrantyName).build()
        val intDuration = duration.toInt()
        val calendar = GregorianCalendar.getInstance()
        calendar.timeInMillis = warranty.expirationDate
        when (timeUnit) {
            "Days" -> calendar.add(GregorianCalendar.DAY_OF_MONTH, -intDuration)
            "Weeks" -> calendar.add(GregorianCalendar.DAY_OF_MONTH, -intDuration * 7)
            "Months" -> calendar.add(GregorianCalendar.MONTH, -intDuration)
            "Years" -> calendar.add(GregorianCalendar.YEAR, -intDuration)
        }
        val days = getDaysFromDateMillis(calendar.timeInMillis)

        if (days > 0) {
            val request = OneTimeWorkRequestBuilder<ExpirationNotifierWorker>()
                .setInitialDelay(days, TimeUnit.DAYS)
                .setInputData(data)
                .build()

            //Enqueue unique work with warranty's id as unique identifier
            workManager.enqueueUniqueWork(
                warranty.id.toString(),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        return days
    }

    internal suspend fun onCheckWorkExist() {
        displayModel.value?.takeIf { it.isValid() }?.let {
            val workInfo = workManager.getWorkInfosForUniqueWork(it.id.toString()).await()

            if (workInfo.size == 1) {
                val workState = workInfo[0].state

                _workExist.postValue(
                    workState == WorkInfo.State.BLOCKED ||
                            workState == WorkInfo.State.ENQUEUED ||
                            workState == WorkInfo.State.RUNNING
                )
                return
            }
        }

        _workExist.postValue(false)
        return
    }

    internal fun cancelWork() {
        displayModel.value?.takeIf { it.isValid() }?.let { workManager.cancelUniqueWork(it.id.toString()) }
    }

    fun calculateExpirationDate(duration: String, timeUnit: String): Long {
        inputModel.apply {
            val intDuration = (duration.takeIf { it.isNotBlank() } ?: "0").toInt()
            val calendar = GregorianCalendar.getInstance()
            calendar.timeInMillis = purchaseDate
            when (timeUnit) {
                "Days" -> calendar.add(GregorianCalendar.DAY_OF_MONTH, intDuration)
                "Weeks" -> calendar.add(GregorianCalendar.DAY_OF_MONTH, intDuration * 7)
                "Months" -> calendar.add(GregorianCalendar.MONTH, intDuration)
                "Years" -> calendar.add(GregorianCalendar.YEAR, intDuration)
            }
            return calendar.timeInMillis
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearCache(getApplication())
    }

    fun getDuration(): Pair<String, String> {
        inputModel.apply {
            val startDate = localDateFromMillis(purchaseDate)
            val endDate = localDateFromMillis(expirationDate)
            val period = Period.between(startDate, endDate)
            return if (period.years > 0 && period.months == 0 && period.days == 0) {
                Pair(period.years.toString(), "Years")
            } else if (period.years == 0 && period.months > 0 && period.days == 0) {
                Pair(period.months.toString(), "Months")
            } else {
                val days = ChronoUnit.DAYS.between(startDate, endDate).toInt()
                if (days % 7 == 0) {
                    Pair((days / 7).toString(), "Weeks")
                } else {
                    Pair(days.toString(), "Days")
                }
            }
        }
    }

    fun onImgCopiedToTemp(file: File) {
        _tempImage.postValue(file.compressImage())
    }
}