package com.neyhuansikoko.warrantylogger

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ExpirationNotifierWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val notificationId: Int = NOTIFICATION_ID

    override fun doWork(): Result {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent
            .getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val warrantyName = inputData.getString(nameKey)

        val builder = NotificationCompat.Builder(applicationContext, WarrantyLoggerApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle("About to expire!")
            .setContentText("The warranty \"$warrantyName\" is about to expire")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(notificationId, builder.build())
        }

        return Result.success()
    }

    companion object {
        const val nameKey = "warranty_name"
    }
}