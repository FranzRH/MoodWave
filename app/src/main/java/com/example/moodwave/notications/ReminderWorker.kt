package com.example.moodwave.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.moodwave.MainActivity
import com.example.moodwave.R

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID =
            "moodwave_daily_reminder"

        private const val NOTIFICATION_ID = 2001
    }

    override fun doWork(): Result {
        crearCanalNotificacion()
        mostrarNotificacion()

        return Result.success()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios diarios",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Recordatorio para registrar tu estado emocional en MoodWave"
            }

            val manager =
                applicationContext.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarNotificacion() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion =
            NotificationCompat.Builder(
                applicationContext,
                CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("MoodWave 🌊")
                .setContentText(
                    "¿Cómo te sientes hoy? Registra tu estado de ánimo y energía."
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "¿Cómo te sientes hoy? Tómate un momento para registrar tu estado de ánimo y nivel de energía."
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(applicationContext)
            .notify(
                NOTIFICATION_ID,
                notificacion
            )
    }
}
