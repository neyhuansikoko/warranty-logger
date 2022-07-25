package com.neyhuansikoko.warrantylogger.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warranty")
data class Warranty(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "warranty_name") val warrantyName: String,
    @ColumnInfo(name = "expiration_date") val expirationDate: Long,
    @ColumnInfo(name = "image_uri") val imageUri: String?
)