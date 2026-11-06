package com.example.moodwave.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val NOMBRE_TRABAJO =
        "moodwave_recordatorio_diario"

    fun programar(
        context: Context,
        hora: Int
    ) {
        val retrasoInicial =
            calcularRetrasoHastaHora(hora)

        val trabajo =
            PeriodicWorkRequestBuilder<ReminderWorker>(
                24,
                TimeUnit.HOURS
            )
                .setInitialDelay(
                    retrasoInicial,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager
            .getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                NOMBRE_TRABAJO,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                trabajo
            )
    }


    fun probarAhora(
        context: Context
    ) {
        val trabajo =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .build()

        WorkManager
            .getInstance(context.applicationContext)
            .enqueue(trabajo)
    }

    fun cancelar(
        context: Context
    ) {
        WorkManager
            .getInstance(context.applicationContext)
            .cancelUniqueWork(NOMBRE_TRABAJO)
    }

    private fun calcularRetrasoHastaHora(
        hora: Int
    ): Long {
        val ahora = Calendar.getInstance()

        val siguiente =
            Calendar.getInstance().apply {
                set(
                    Calendar.HOUR_OF_DAY,
                    hora.coerceIn(0, 23)
                )
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= ahora.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

        return siguiente.timeInMillis - ahora.timeInMillis
    }
}
