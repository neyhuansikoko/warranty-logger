package com.neyhuansikoko.warrantylogger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.WarrantyDao
import com.neyhuansikoko.warrantylogger.database.WarrantyRoomDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var warrantyDao: WarrantyDao
    private lateinit var warrantyRoomDatabase: WarrantyRoomDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        warrantyRoomDatabase = Room.inMemoryDatabaseBuilder(
            context, WarrantyRoomDatabase::class.java).allowMainThreadQueries().build()
        warrantyDao = warrantyRoomDatabase.warrantyDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        warrantyRoomDatabase.close()
    }
}