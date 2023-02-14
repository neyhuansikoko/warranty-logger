package com.neyhuansikoko.warrantylogger.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neyhuansikoko.warrantylogger.*
import java.time.LocalDate
import java.time.Period

@Entity(tableName = "warranties")
data class Warranty(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "warranty_id")
    val warrantyId: Int = 0,
    @ColumnInfo(name = "warranty_name") var warrantyName: String,
    @ColumnInfo(name = "note") var note: String = "",
    @ColumnInfo(name = "expiration_date") var expirationDate: Long,
    @ColumnInfo(name = "purchase_date") var purchaseDate: Long = Long.MIN_VALUE,
    @ColumnInfo(name = "created_at") val createdDate: Long = Long.MIN_VALUE,
    @ColumnInfo(name = "modified_last_at") var modifiedDate: Long = Long.MIN_VALUE,
)

fun Warranty.getRemainingDays(): String {
    val days = getDaysFromDateMillis(expirationDate).toInt()
    return if (days > 1) {
        "$days days"
    } else if (days == 1) {
        "$days day"
    } else {
        "expired"
    }
}

fun Warranty.getRemainingDate(): String {
    val period = Period.between(LocalDate.now(), localDateFromMillis(expirationDate))
    val stringBuilder = StringBuilder()
    if (period.isZero || period.isNegative) {
        stringBuilder.append("expired")
    } else {
        if (period.years > 0) {
            stringBuilder.append("${period.years} ${if(period.years > 1) "years" else "year"} ")
        }
        if (period.months > 0) {
            stringBuilder.append("${period.months} ${if(period.months > 1) "months" else "month"} ")
        }
        if (period.days > 0) {
            stringBuilder.append("${period.days} ${if(period.days > 1) "days" else "day"}")
        }
    }

    return stringBuilder.toString()
}

fun Warranty.isValid(): Boolean {
    return this.warrantyId > 0
}

//Used to delete stored image file
//fun Warranty.deleteImageFile(context: Context) {
//    this.image?.let { image ->
//        getImageFile(context, image)?.delete()
//        this.image = null
//    }
//}