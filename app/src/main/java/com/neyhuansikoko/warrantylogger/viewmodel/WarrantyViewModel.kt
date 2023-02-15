package com.neyhuansikoko.warrantylogger.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.work.*
import com.neyhuansikoko.warrantylogger.*
import com.neyhuansikoko.warrantylogger.database.model.Image
import com.neyhuansikoko.warrantylogger.database.model.Warranty
import com.neyhuansikoko.warrantylogger.database.model.isValid
import com.neyhuansikoko.warrantylogger.repository.WarrantyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WarrantyViewModel @Inject constructor(
    application: Application,
    private val warrantyRepository: WarrantyRepository
): AndroidViewModel(application) {

    enum class WarrantyAttribute { NAME, DATE }
    enum class WarrantySort { ASC, DSC }

    val displayModel: MutableLiveData<Warranty> = MutableLiveData(DEFAULT_MODEL)
    var inputModel: Warranty = DEFAULT_MODEL

    val allWarranties: LiveData<List<Warranty>> = warrantyRepository.getAllWarranties().asLiveData()
    val filterWarranties: MutableLiveData<List<Warranty>> = MutableLiveData()
    val mediatorWarranties: MediatorLiveData<List<Warranty>> = MediatorLiveData()
    val mediatorSize: Int get() = mediatorWarranties.value?.size ?: 0

    private val _workExist: MutableLiveData<Boolean> = MutableLiveData()
    val workExist: LiveData<Boolean> get() = _workExist

    private val _imageList: MutableLiveData<List<Image>> = MutableLiveData(emptyList())
    val imageList: LiveData<List<Image>> get() = _imageList

    private val _warrantyImageList: MutableLiveData<List<Image>> = MutableLiveData(emptyList())
    val warrantyImageList: LiveData<List<Image>> get() = _warrantyImageList

    val deleteList: MutableLiveData<MutableList<Warranty>> = MutableLiveData(mutableListOf())
    val deleteSize: Int get() = deleteList.value?.size ?: 0

    private val imageUriList: MutableList<Uri> = mutableListOf()
    fun getImageUriCount() = imageUriList.size

    private val _cameraImageCount: MutableLiveData<Int> = MutableLiveData(0)
    val cameraImageCount: LiveData<Int> get() = _cameraImageCount

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
                list.sortedBy { it.warrantyName.lowercase() }
            } else {
                list.sortedByDescending { it.warrantyName.lowercase() }
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
            val warrantyBind = inputModel.copy()
            val listBind = _imageList.value!!
            viewModelScope.launch {
                val warrantyId = warrantyRepository.insertWarranty(warrantyBind).toInt()

                if (listBind.isNotEmpty()) {
                    val persistImageList = saveTempImage(listBind, warrantyId)
                    warrantyRepository.insertAllImages(*persistImageList.toTypedArray())
                    onImagesPersisted()
                }
            }
        }
    }

    fun updateWarranty() {
        if (inputModel.isValid()) {
            val warrantyBind = inputModel.copy()
            val listBind = _imageList.value!!
            viewModelScope.launch {
                warrantyRepository.updateWarranty(warrantyBind)

                val persistImageList = saveTempImage(listBind, warrantyBind.warrantyId)
                warrantyRepository.insertAllImages(*persistImageList.toTypedArray())
                onImagesPersisted()

                displayModel.value = inputModel.copy()
            }
        }
    }

    private suspend fun onImagesPersisted() {
        _imageList.value = emptyList()
        withContext(Dispatchers.IO) {
            clearCache(getApplication<Application>().applicationContext)
        }
    }

    fun deleteWarranty() {
        if (inputModel.isValid()) {
            val warrantyBind = inputModel.copy()
            val warrantyImageListBind = _warrantyImageList.value!!
            viewModelScope.launch {
                launch {
                    warrantyRepository.deleteWarranty(warrantyBind)
                }
                warrantyImageListBind.forEach { image ->
                    launch {
                        deleteImageFile(image)
                    }
                }
            }
            inputModel = DEFAULT_MODEL
        }
    }

    private suspend fun deleteImageFile(image: Image) {
        withContext(Dispatchers.IO) {
            try {
                val imageFile = image.imageUri.toUri().toFile()
                val thumbnailFile = image.thumbnailUri.toUri().toFile()
                imageFile.delete()
                thumbnailFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSelectedWarranty() {
        deleteList.value?.let { listWarrantyBind ->
            if (listWarrantyBind.isNotEmpty()) {
                viewModelScope.launch {
                    val listImageDelete: MutableList<Image> = mutableListOf()
                    val warrantyIdList: List<Int> = listWarrantyBind.map { it.warrantyId }
                    val channel = Channel<List<Image>>(warrantyIdList.size)

                    val jobs = warrantyIdList.map { id ->
                        launch {
                            val imagesOfWarranty = warrantyRepository.getAllImagesForWarranty(id)
                            channel.send(imagesOfWarranty)
                        }
                    }

                    launch {
                        repeat(warrantyIdList.size) {
                            listImageDelete.addAll(channel.receive())
                        }

                        listImageDelete.forEach { image ->
                            launch {
                                deleteImageFile(image)
                            }
                        }
                    }

                    jobs.joinAll()
                    warrantyRepository.deleteSelectedWarranties(warrantyIdList)
                }
            }
        }
    }

    //Copy temp image to non-cache directory
     private suspend fun saveTempImage(list: List<Image>, warrantyId: Int) = withContext(Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        val imageDir = File(
            context.filesDir,
            IMAGE_DIR
        ).also { it.mkdir() }

        val persistImageList = list.map { image ->
            val imageUri = image.imageUri.toUri()
            val newImage = File(imageDir, getUriFilename(context, imageUri))
            newImage.outputStream().use { newOS ->
                context.contentResolver.openInputStream(imageUri)?.use { imageIS ->
                    imageIS.copyTo(newOS)
                }
            }

            val thumbnailUri = image.thumbnailUri.toUri()
            val newThumbnail = File(imageDir, getUriFilename(context, thumbnailUri))
            newThumbnail.outputStream().use { newOS ->
                context.contentResolver.openInputStream(thumbnailUri)?.use { thumbnailIS ->
                    thumbnailIS.copyTo(newOS)
                }
            }
//            val compressedNewImage = withContext(Dispatchers.Default) {
//                newImage.compressImage()
//            }
            image.copy(
                imageId = 0,
                warrantyId = warrantyId,
                imageUri = newImage.toUri().toString(),
                thumbnailUri = newThumbnail.toUri().toString()
            )
        }
        persistImageList
    }

    fun clearTempImage() {
        _imageList.value = emptyList()
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
                warranty.warrantyId.toString(),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        return days
    }

    internal suspend fun onCheckWorkExist() {
        displayModel.value?.takeIf { it.isValid() }?.let {
            val workInfo = workManager.getWorkInfosForUniqueWork(it.warrantyId.toString()).await()

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
        displayModel.value?.takeIf { it.isValid() }?.let { workManager.cancelUniqueWork(it.warrantyId.toString()) }
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

    fun onImagesRetrieved(imageUriList: List<Uri>) {
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            val tempImageList = imageUriList.map { imageUri ->
                val tempFile = File(context.cacheDir, getUniqueName() + TEMP_IMAGE_SUFFIX)
                tempFile.outputStream().use { tempOS ->
                    context.contentResolver.openInputStream(imageUri).use { imageIS ->
                        imageIS?.copyTo(tempOS)
                    }
                }
                tempFile.toUri()
            }
            appendImageList(tempImageList)
        }
    }

    private suspend fun appendImageList(imageUriList: List<Uri>) {
        var idCount = _imageList.value!!.size
        val imageList = imageUriList.map { imageUri ->
            val thumbnailUri = withContext(Dispatchers.Default) {
                 createThumbnail(
                    getApplication<Application>().applicationContext,
                    imageUri
                )
            }

            Image(
                imageId = ++idCount,
                warrantyId = 0,
                imageUri = imageUri.toString(),
                thumbnailUri = thumbnailUri.toString(),
                createdDate = nowMillis
            )
        }
        _imageList.apply {
            this.postValue(this.value!! + imageList)
        }
    }

    fun onImagesTaken(imageUri: Uri) {
        imageUriList.add(imageUri)
        _cameraImageCount.apply {
            this.postValue(this.value!!.inc())
        }
    }

    fun onCheckCameraImages() {
        viewModelScope.launch {
            appendImageList(imageUriList)
            imageUriList.clear()
        }
    }

    fun onResetImageCount() {
        imageUriList.clear()
        _cameraImageCount.postValue(0)
    }

    fun onCheckWarrantyImages(warrantyId: Int) {
        viewModelScope.launch {
            warrantyRepository.getFlowOfAllImagesForWarranty(warrantyId).collect { list ->
                _warrantyImageList.postValue(list)
            }
        }
    }

    fun onTempImageDelete(image: Image) {
        _imageList.value!!.let { list ->
            _imageList.postValue(list.minus(image))
        }
    }

    fun onPersistImageDelete(image: Image) {
        viewModelScope.launch {
            deleteImageFile(image)
            warrantyRepository.deleteSelectedImages(image)
        }
    }
}