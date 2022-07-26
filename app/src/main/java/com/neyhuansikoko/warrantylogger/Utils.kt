package com.neyhuansikoko.warrantylogger

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.text.SimpleDateFormat

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy").format(dateMillis)

fun getFileNameFromUri(uri: Uri, context: Context): String? {
    try {
        context.contentResolver.query(
            uri, null, null, null, null
        )?.use { cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data,
             * and display it.
             */
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val fileName = cursor.getString(nameIndex)
            cursor.close()

            return fileName
        }
        return null
    } catch (e: Exception) {
        return null
    }
}

fun getFilePathFromUri(uri: Uri, context: Context): String? {
    try {
        context.contentResolver.query(
            uri, null, null, null, null
        )?.use { cursor ->
            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data,
             * and display it.
             */
            val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val filePath = cursor.getString(pathIndex)
            cursor.close()

            return filePath
        }
        return null
    } catch (e: Exception) {
        return null
    }
}

fun deleteFileByUri(uri: Uri, context: Context) {
    getFilePathFromUri(uri, context)?.let { filePath ->
        val file = File(filePath)
        if (file.exists()) file.delete()
    }
}