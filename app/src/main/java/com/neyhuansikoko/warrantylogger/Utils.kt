package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.util.Log
import com.google.android.material.datepicker.MaterialDatePicker
import com.neyhuansikoko.warrantylogger.database.Warranty
import java.io.File
import java.text.SimpleDateFormat

fun log(message: String) {
    Log.d("Test", message)
}

var DEFAULT_DATE_SELECTION: Long = MaterialDatePicker.todayInUtcMilliseconds() + DAY_MILLIS

private val _DEFAULT_MODEL = Warranty(
    warrantyName = "",
    expirationDate = 0,
    image = null
)
val DEFAULT_MODEL get() = _DEFAULT_MODEL.copy()

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy").format(dateMillis)

fun getImageFile(context: Context, image: String): File? {
    val imageDir = File(context.filesDir, IMAGE_DIR)
    val imageFile = File(imageDir, image)

    return if (imageFile.exists()) {
        imageFile
    } else {
        null
    }
}

//Return file even if it doesn't exist
fun getImageFileAbs(context: Context, image: String): File {
    val imageDir = File(context.filesDir, IMAGE_DIR)
    return File(imageDir, image)
}

fun getImageFileFromCache(context: Context, image: String): File? {
    val imageFile = File(context.cacheDir, image)

    return if (imageFile.exists()) {
        imageFile
    } else {
        null
    }
}