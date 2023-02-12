package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.snackbar.Snackbar
import com.neyhuansikoko.warrantylogger.database.Warranty
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

fun log(message: String) {
    Log.d("Test", message)
}

val nowMillis get() = GregorianCalendar.getInstance().timeInMillis

fun getDefaultDateSelection(): Long {
    val calendar = GregorianCalendar.getInstance()
    calendar.add(GregorianCalendar.DAY_OF_MONTH, 1)
    return calendar.timeInMillis
}

val EMPTY_DATE_CONSTRAINT: CalendarConstraints = CalendarConstraints.Builder().build()

private val _DEFAULT_MODEL = Warranty(
    warrantyName = "",
    purchaseDate = Long.MIN_VALUE,
    expirationDate = Long.MIN_VALUE,
    image = null
)
val DEFAULT_MODEL get() = _DEFAULT_MODEL.copy(
    purchaseDate = nowMillis
)

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(dateMillis)
fun formatDateTimeMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(dateMillis)

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

fun getDaysFromDateMillis(dateMillis: Long): Long {
    return ChronoUnit.DAYS.between(LocalDate.now(), localDateFromMillis(dateMillis))
}

fun localDateFromMillis(millis: Long): LocalDate {
    val calendar = GregorianCalendar.getInstance()
    calendar.timeInMillis = millis
    return LocalDate.of(
        calendar.get(GregorianCalendar.YEAR),
        calendar.get(GregorianCalendar.MONTH) + 1, //Calendar month start at 0 expect start at 1
        calendar.get(GregorianCalendar.DAY_OF_MONTH)
    )
}

fun displayShortMessage(view: View, message: String): Snackbar {
    return Snackbar.make(
        view,
        message,
        Snackbar.LENGTH_SHORT
    ).also { it.show() }
}

fun displayLongMessage(view: View, message: String): Snackbar {
    return Snackbar.make(
        view,
        message,
        Snackbar.LENGTH_LONG
    ).also { it.show() }
}

fun getLastModifiedDate(file: File): String? {
    return if (file.exists()) {
        val dateInMillis = file.lastModified()
        formatDateTimeMillis(dateInMillis)
    } else {
        null
    }
}