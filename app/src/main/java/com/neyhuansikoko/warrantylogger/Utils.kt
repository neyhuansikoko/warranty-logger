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
import com.google.android.material.datepicker.MaterialDatePicker
import com.neyhuansikoko.warrantylogger.database.Warranty
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

fun log(message: String) {
    Log.d("Test", message)
}

fun logPing() {
    log("Ping!")
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