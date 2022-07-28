package com.neyhuansikoko.warrantylogger.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neyhuansikoko.warrantylogger.*
import java.util.*
import kotlin.math.ceil

@Entity(tableName = "warranty")
data class Warranty(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "warranty_name") var warrantyName: String,
    @ColumnInfo(name = "expiration_date") var expirationDate: Long,
    @ColumnInfo(name = "image") var image: String?
)

fun Warranty.getRemainingTime(): String {
    val currentDay = Calendar.getInstance().timeInMillis
    val remainingDays = (ceil(expirationDate.toDouble() / DAY_MILLIS) - ceil(currentDay.toDouble() / DAY_MILLIS)).toInt() + 1
    val remainingMonths = (ceil(expirationDate.toDouble() / MONTH_MILLIS) - ceil(currentDay.toDouble() / MONTH_MILLIS)).toInt()
    val remainingYear = (ceil(expirationDate.toDouble() / YEAR_MILLIS) - ceil(currentDay.toDouble() / YEAR_MILLIS)).toInt()

    return if (remainingDays < 1) {
        "expired"
    } else if (remainingDays < 30) {
        "$remainingDays ${if (remainingDays > 1) "days" else "day"}"
    } else if (remainingMonths < 12) {
        "$remainingMonths ${if (remainingMonths > 1) "months" else "month"}"
    } else {
        "$remainingYear ${if (remainingYear > 1) "years" else "year"}"
    }
}

fun Warranty.isValid(): Boolean {
    return this.id > DEFAULT_MODEL.id
}

fun Warranty.same(warranty: Warranty): Boolean {
    return this.id == warranty.id
}

//Used to delete stored image file
fun Warranty.deleteImageFile(context: Context) {
    this.image?.let { image ->
        getImageFile(context.applicationContext, image)?.delete()
        this.image = null
    }
}