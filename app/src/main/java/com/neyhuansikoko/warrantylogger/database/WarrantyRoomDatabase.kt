package com.neyhuansikoko.warrantylogger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Warranty::class], version = 1, exportSchema = false)
abstract class WarrantyRoomDatabase : RoomDatabase() {

    abstract fun warrantyDao(): WarrantyDao

    companion object {

        @Volatile
        private var INSTANCE: WarrantyRoomDatabase? = null

        fun getDatabase(context: Context): WarrantyRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarrantyRoomDatabase::class.java,
                    "item_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                instance
            }
        }
    }
}