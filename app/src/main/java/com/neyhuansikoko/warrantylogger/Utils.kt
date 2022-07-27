package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.util.Log
import com.neyhuansikoko.warrantylogger.database.Warranty
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

fun log(message: String) {
    Log.d("Test", message)
}

const val DAY_MILLIS: Long = 86400000
const val MONTH_MILLIS: Long = 2629800000
const val YEAR_MILLIS: Long = 31557600000


fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy").format(dateMillis)

fun getImageFile(context: Context, image: String): File {
    val imageDir = File(context.filesDir, IMAGE_DIR)
    return File(imageDir, image)
}

fun getImageFileFromCache(context: Context, image: String): File {
    return File(context.cacheDir, image)
}

fun Warranty.getRemainingTime(): String {
    val currentDay = Calendar.getInstance().timeInMillis
    val remainingDays = (ceil(expirationDate.toDouble() / DAY_MILLIS) - ceil(currentDay.toDouble() / DAY_MILLIS)).toInt() + 1
    val remainingMonths = (ceil(expirationDate.toDouble() / MONTH_MILLIS) - ceil(currentDay.toDouble() / MONTH_MILLIS)).toInt()
    val remainingYear = (ceil(expirationDate.toDouble() / YEAR_MILLIS) - ceil(currentDay.toDouble() / YEAR_MILLIS)).toInt()

    return if (remainingDays < 1) {
        "expired"
    } else if (remainingDays < 30) {
        "$remainingDays ${if (remainingDays > 1) "days" else "day"}"
    } else if (remainingMonths < 12) {
        "$remainingMonths ${if (remainingMonths > 1) "months" else "month"}"
    } else {
        "$remainingYear ${if (remainingYear > 1) "years" else "year"}"
    }
}