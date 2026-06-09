package com.example.sniffer.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sniffer.R
import com.example.sniffer.domain.NotificationService

object AndroidNotificationService : NotificationService {

    const val CHANNEL_ID   = "sniffer_smoke_alert"
    private const val CHANNEL_NAME = "Smoke & Danger Alerts"
    private const val NOTIF_ID     = 1001

    // Context is passed per-call to avoid leaking an Activity context.
    // Always supply applicationContext from the ViewModel.
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    override fun createChannel() {
        val ctx = appContext ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for smoke, fire, or chemical vapour detection"
                enableVibration(true)
                enableLights(true)
            }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun sendSmokeAlert(iaq: Int, temperature: Int) {
        val ctx = appContext ?: return
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Дым / Газ!")
            .setContentText("IAQ: $iaq  •  Темп: $temperature°C — Проверьте плиту.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Высокий показатель качества воздуха $iaq вместе с " +
                            "температурой (${temperature}°C)." +
                            "Проверьте помещение на наличие дыма / сгоревшей еды / утечки газа немедленно!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — in-app alert still shows
        }
    }
}