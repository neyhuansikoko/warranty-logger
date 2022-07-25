package com.neyhuansikoko.warrantylogger

import java.text.SimpleDateFormat

fun formatDateMillis(dateMillis: Long): String = SimpleDateFormat("dd/MM/yyyy").format(dateMillis)