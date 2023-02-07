package com.neyhuansikoko.warrantylogger.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.neyhuansikoko.warrantylogger.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val warrantyApp = getApplication<WarrantyLoggerApplication>()
    private val appContext get() = warrantyApp.applicationContext
    private val database get() = warrantyApp.database

    private var _backupCompleted: MutableLiveData<String?> = MutableLiveData(
        getLastModifiedDate(File(appContext.filesDir, BACKUP_ZIP))
    )
    val backupCompleted: LiveData<String?> get() = _backupCompleted

    fun backupDatabase(callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var successful = false

            val outputZipFile = File(appContext.filesDir, BACKUP_ZIP)

            val dbFile = appContext.getDatabasePath(DATABASE_NAME)
            val dbWalFile = File(dbFile.path + WAL_FILE_SUFFIX)
            val dbShmFile = File(dbFile.path + SHM_FILE_SUFFIX)

            val imageDir = File(appContext.filesDir, IMAGE_DIR)

            checkpoint()

            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zipOS ->
                    val backupDbFileName = dbFile.name + BACKUP_FILE_SUFFIX

                    //Put DB file to ZIP
                    zipOS.putNextEntry(ZipEntry(backupDbFileName))
                    dbFile.inputStream().use { it.copyTo(zipOS) }

                    //Put DB Wal file to ZIP
                    if (dbWalFile.exists()) {
                        zipOS.putNextEntry(ZipEntry(backupDbFileName + WAL_FILE_SUFFIX))
                        dbWalFile.inputStream().use { it.copyTo(zipOS) }
                    }

                    //Put DB Shm file to ZIP
                    if (dbShmFile.exists()) {
                        zipOS.putNextEntry(ZipEntry(backupDbFileName + SHM_FILE_SUFFIX))
                        dbShmFile.inputStream().use { it.copyTo(zipOS) }
                    }

                    //Put image directory to ZIP
                    if (imageDir.exists()) {
                        imageDir.walkTopDown().forEach { file ->
                            zipOS.putNextEntry(ZipEntry(
                                if (file.isDirectory) {
                                    "$IMAGE_DIR/"
                                } else {
                                    "$IMAGE_DIR/${file.name}"
                                }
                            ))
                            if (file.isFile) {
                                file.inputStream().use { it.copyTo(zipOS) }
                            }
                        }
                    }
                }

                _backupCompleted.postValue(getLastModifiedDate(outputZipFile))
                successful = true

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    callback(successful)
                }
            }
        }
    }

    fun restoreDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!File(appContext.filesDir, BACKUP_ZIP).exists()) {
                return@launch
            }
            val dbPath = database.openHelper.readableDatabase.path
            val dbFile = File(dbPath)
            val dbWalFile = File(dbFile.path + WAL_FILE_SUFFIX)
            val dbShmFile = File(dbFile.path + SHM_FILE_SUFFIX)

            val imageDir = File(appContext.filesDir, IMAGE_DIR)
            imageDir.deleteRecursively()
            imageDir.mkdir()

            val zipFile = ZipFile(File(appContext.filesDir, BACKUP_ZIP))

            try {
                val backupDbName = DATABASE_NAME + BACKUP_FILE_SUFFIX
                //Copy backup DB file to current DB file
                zipToFile(zipFile, zipFile.getEntry(backupDbName), dbFile)

                //Copy backup DB Wal file to current DB Wal file
                val walZipEntry = zipFile.getEntry(backupDbName + WAL_FILE_SUFFIX)
                if (walZipEntry != null) {
                    zipToFile(zipFile, walZipEntry, dbWalFile)
                }

                //Copy backup DB Shm file to current DB Shm file
                val shmZipEntry = zipFile.getEntry(backupDbName + SHM_FILE_SUFFIX)
                if (shmZipEntry != null) {
                    zipToFile(zipFile, shmZipEntry, dbShmFile)
                }

                //Copy backup image directory to current image directory (which should be empty by now)
                zipFile.entries().toList()
                    .filter { it.name.startsWith("$IMAGE_DIR/") && !it.isDirectory }
                    .forEach { entry ->
                        log(entry.name)
                        val image = File(appContext.filesDir, entry.name)
                        zipToFile(zipFile, entry, image)
                    }

                checkpoint()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            restart()
        }
    }

    fun exportBackupFile(callback: (String) -> Unit) {
        if (!File(appContext.filesDir, BACKUP_ZIP).exists()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val backupZip = File(appContext.filesDir, BACKUP_ZIP)

            val uniqueName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())

            val externalBackup = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "${backupZip.nameWithoutExtension}_${uniqueName}.zip"
            )

            backupZip.copyTo(externalBackup, true)

            withContext(Dispatchers.Main) {
                callback(externalBackup.canonicalPath)
            }
        }
    }

    private fun checkpoint() {
        val writableDb = database.openHelper.writableDatabase
        writableDb.query("PRAGMA wal_checkpoint(TRUNCATE);", null)
    }

    fun updateBackupDate() {
        _backupCompleted.postValue(getLastModifiedDate(File(appContext.filesDir, BACKUP_ZIP)))
    }

    private fun getLastModifiedDate(file: File): String? {
        return if (file.exists()) {
            val dateInMillis = file.lastModified()
            formatDateTimeMillis(dateInMillis)
        } else {
            null
        }
    }

    private fun restart() {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        appContext.startActivity(intent)
        exitProcess(0)
    }

    private fun zipToFile(zipFile: ZipFile, zipEntry: ZipEntry, file: File) {
        file.outputStream().use { fileOS ->
            zipFile.getInputStream(zipEntry).use { zipEntryIS ->
                zipEntryIS.copyTo(fileOS)
            }
        }
    }
}