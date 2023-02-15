package com.neyhuansikoko.warrantylogger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neyhuansikoko.warrantylogger.DATABASE_NAME
import com.neyhuansikoko.warrantylogger.database.dao.ImageDao
import com.neyhuansikoko.warrantylogger.database.dao.WarrantyDao
import com.neyhuansikoko.warrantylogger.database.model.Image
import com.neyhuansikoko.warrantylogger.database.model.Warranty

@Database(entities = [Warranty::class, Image::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun warrantyDao(): WarrantyDao
    abstract fun imageDao(): ImageDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() //TODO: Remove this on production
                    .build()
                INSTANCE = instance

                instance
            }
        }
    }
}