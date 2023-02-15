package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.MutableLiveData
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.snackbar.Snackbar
import com.neyhuansikoko.warrantylogger.database.model.Warranty
import java.io.File
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
    expirationDate = Long.MIN_VALUE
)
val DEFAULT_MODEL get() = _DEFAULT_MODEL.copy(
    purchaseDate = nowMillis
)

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(dateMillis)
fun formatDateTimeMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(dateMillis)

fun clearCache(context: Context) {
    val dir = context.cacheDir
    dir.deleteRecursively()
}

fun getUniqueName(): String {
    return SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
}

//fun File.compressImage(): File {
//    val exifOrientation = ExifInterface(this).getAttribute(ExifInterface.TAG_ORIENTATION)
//    val bitmap = BitmapFactory.decodeFile(this.path)
//    var out: FileOutputStream? = null
//    try {
//        out = FileOutputStream(this)
//        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY /* ignored for PNG */, out)
//    } finally {
//        out?.let {
//            try {
//                it.close()
//            } catch (ignore: IOException) {
//            }
//        }
//    }
//    ExifInterface(this).apply {
//        setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation)
//        saveAttributes()
//    }
//
//    return this
//}

fun getUriFilename(context: Context, uri: Uri): String {
    var name = NO_NAME_IMAGE
    try {
        name = uri.toFile().name
    } catch (e: Exception) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

fun String.getFilenameWithoutExtension(): String {
    return this.removeRange(this.lastIndexOf('.'), this.length)
}

fun createThumbnail(context: Context, imageUri: Uri): Uri {
    val thumbnailFile = File(
        context.cacheDir,
        getUriFilename(context, imageUri).getFilenameWithoutExtension() + THUMBNAIL_SUFFIX + TEMP_IMAGE_SUFFIX
    )
    thumbnailFile.outputStream().use { thumbOS ->
        context.contentResolver.openInputStream(imageUri)?.use { imageIS ->
            imageIS.copyTo(thumbOS)
        }
    }

    val exifOrientation = ExifInterface(thumbnailFile).getAttribute(ExifInterface.TAG_ORIENTATION)
    var bitmap = BitmapFactory.decodeFile(imageUri.path)

    val width: Int
    val height: Int
    if (bitmap.height > bitmap.width) {
        width = bitmap.width / (bitmap.height / THUMBNAIL_SIZE)
        height = THUMBNAIL_SIZE
    } else {
        height = bitmap.height / (bitmap.width / THUMBNAIL_SIZE)
        width = THUMBNAIL_SIZE
    }
    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)

    thumbnailFile.outputStream().use { thumbOS ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100 /* ignored for PNG */, thumbOS)
    }
    bitmap.recycle()

    ExifInterface(thumbnailFile).apply {
        setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation)
        saveAttributes()
    }
    return thumbnailFile.toUri()
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