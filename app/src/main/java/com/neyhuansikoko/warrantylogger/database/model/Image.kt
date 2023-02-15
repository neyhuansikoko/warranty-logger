package com.neyhuansikoko.warrantylogger.database.model

import androidx.room.*

@Entity(tableName = "images", foreignKeys = [
    ForeignKey(entity = Warranty::class, childColumns = ["warranty_id"], parentColumns = ["warranty_id"], onDelete = ForeignKey.CASCADE)
], indices = [Index("warranty_id")])
data class Image(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "image_id")
    val imageId: Int,
    @ColumnInfo(name = "warranty_id") val warrantyId: Int,
    @ColumnInfo(name = "image_uri") val imageUri: String,
    @ColumnInfo(name = "thumbnail_uri") val thumbnailUri: String,
    @ColumnInfo(name = "created_date") val createdDate: Long
)