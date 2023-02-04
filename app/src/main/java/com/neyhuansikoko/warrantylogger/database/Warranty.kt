package com.neyhuansikoko.warrantylogger.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neyhuansikoko.warrantylogger.*

@Entity(tableName = "warranty")
data class Warranty(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "warranty_name") var warrantyName: String,
    @ColumnInfo(name = "note") var note: String = "",
    @ColumnInfo(name = "expiration_date") var expirationDate: Long,
    @ColumnInfo(name = "purchase_date") var purchaseDate: Long = Long.MIN_VALUE,
    @ColumnInfo(name = "created_at") val createdDate: Long = Long.MIN_VALUE,
    @ColumnInfo(name = "modified_last_at") var modifiedDate: Long = Long.MIN_VALUE,
    @ColumnInfo(name = "image") var image: String?
)

fun Warranty.getRemainingTime(): String {
    val days = getDaysFromDateMillis(expirationDate).toInt()
    return if (days > 1) {
        "$days days"
    } else if (days == 1) {
        "$days day"
    } else {
        "expired"
    }
}

fun Warranty.isValid(): Boolean {
    return this.id > 0
}

//Used to delete stored image file
fun Warranty.deleteImageFile(context: Context) {
    this.image?.let { image ->
        getImageFile(context, image)?.delete()
        this.image = null
    }
}