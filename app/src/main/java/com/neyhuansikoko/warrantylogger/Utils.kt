package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat

fun log(message: String) {
    Log.d("Test", message)
}

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy").format(dateMillis)

fun getImageFile(context: Context, image: String): File {
    val imageDir = File(context.filesDir, IMAGE_DIR)
    return File(imageDir, image)
}

fun getImageFileFromCache(context: Context, image: String): File {
    return File(context.cacheDir, image)
}