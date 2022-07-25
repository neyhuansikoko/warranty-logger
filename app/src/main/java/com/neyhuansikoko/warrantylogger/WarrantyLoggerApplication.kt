package com.neyhuansikoko.warrantylogger

import android.app.Application
import com.neyhuansikoko.warrantylogger.database.WarrantyRoomDatabase

class WarrantyLoggerApplication : Application() {
    val database: WarrantyRoomDatabase by lazy { WarrantyRoomDatabase.getDatabase(this) }
}