package com.neyhuansikoko.warrantylogger.database.di

import android.content.Context
import com.neyhuansikoko.warrantylogger.database.AppDatabase
import com.neyhuansikoko.warrantylogger.database.dao.ImageDao
import com.neyhuansikoko.warrantylogger.database.dao.WarrantyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideWarrantyDao(appDatabase: AppDatabase): WarrantyDao {
        return appDatabase.warrantyDao()
    }

    @Provides
    fun provideImageDao(appDatabase: AppDatabase): ImageDao {
        return appDatabase.imageDao()
    }
}