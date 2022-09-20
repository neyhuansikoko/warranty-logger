package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.neyhuansikoko.warrantylogger.database.Warranty
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

fun log(message: String) {
    Log.d("Test", message)
}

fun logPing() {
    log("Ping!")
}

val DEFAULT_DATE_SELECTION: Long get() =  MaterialDatePicker.todayInUtcMilliseconds() + DAY_MILLIS

val EMPTY_DATE_CONSTRAINT: CalendarConstraints = CalendarConstraints.Builder().build()

private val _DEFAULT_MODEL = Warranty(
    warrantyName = "",
    expirationDate = Long.MIN_VALUE,
    image = null
)
val DEFAULT_MODEL get() = _DEFAULT_MODEL.copy()

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy").format(dateMillis)
fun formatDateTimeMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dateMillis)

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

fun clearCache(context: Context) {
    val dir = context.cacheDir
    dir.deleteRecursively()
}

fun File.compressImage(): File {
    val exifOrientation = ExifInterface(this).getAttribute(ExifInterface.TAG_ORIENTATION)
    val bitmap = BitmapFactory.decodeFile(this.path)
    var out: FileOutputStream? = null
    try {
        out = FileOutputStream(this)
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY /* ignored for PNG */, out)
    } finally {
        out?.let {
            try {
                it.close()
            } catch (ignore: IOException) {
            }
        }
    }
    ExifInterface(this).apply {
        setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation)
        saveAttributes()
    }

    return this
}

fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}

fun closeSoftKeyboard(view: View, context: Context) {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun getDaysToDate(date: Long): Long {
    val currentDate = Calendar.getInstance().timeInMillis.floorDiv(DAY_MILLIS)
    val expirationDate = ceil(date.toDouble() / DAY_MILLIS).toLong()

    return expirationDate - currentDate
}

fun inputToDays(duration: Long, timeUnit: String): Long {
    return when (timeUnit) {
        "Days" -> duration
        "Weeks" -> duration * 7
        "Months" -> duration * 30
        else -> duration * 365
    }
}