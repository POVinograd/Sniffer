package com.example.sniffer.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sniffer.R
import com.example.sniffer.data.AndroidNotificationService
import com.example.sniffer.data.FirebaseSensorRepositoryImpl
import com.example.sniffer.domain.AirQualityAdvisor
import com.example.sniffer.domain.SensorData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SensorMonitorService : Service() {

    private val repository = FirebaseSensorRepositoryImpl()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private var previous: SensorData? = null
    private var smokeAlertActive = false

    companion object {
        const val FOREGROUND_NOTIF_ID = 2001
        private const val FOREGROUND_CHANNEL_ID   = "sniffer_monitor"
        private const val FOREGROUND_CHANNEL_NAME = "Air Quality Monitor"

        fun start(context: Context) {
            val intent = Intent(context, SensorMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Log.d("MYSERVICE", "stop() called")
            context.stopService(Intent(context, SensorMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createForegroundChannel()
        AndroidNotificationService.init(applicationContext)
        AndroidNotificationService.createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        if (intent == null && !isLoggedIn) {
            Log.d("MYSERVICE", "Restarted by OS but user is logged out — stopping")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(FOREGROUND_NOTIF_ID, buildForegroundNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        job?.cancel()
        job = scope.launch {
            repository.getSensorStream().collect { history ->
                val current = history.lastOrNull() ?: return@collect

                val isSmoke = AirQualityAdvisor.isSmokeAlert(current, previous)

                // Only fire once per smoke event, reset when air clears
                if (isSmoke && !smokeAlertActive) {
                    AndroidNotificationService.sendSmokeAlert(
                        iaq = current.iaq.toInt(),
                        temperature = current.temperature.toInt()
                    )
                    updateForegroundNotification(danger = true)
                    smokeAlertActive = true
                } else if (!isSmoke) {
                    if (smokeAlertActive) {
                        updateForegroundNotification(danger = false)
                    }
                    smokeAlertActive = false
                }

                previous = current
            }
        }
    }

    override fun onDestroy() {
        Log.d("MYSERVICE", "onDestroy called")
        stopForeground(STOP_FOREGROUND_REMOVE)
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    // Not a bound service
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                FOREGROUND_CHANNEL_NAME,
                // LOW so the persistent notification is silent and unobtrusive
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Sniffer running in the background"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Sniffer — мониторинг воздуха")
            .setContentText("Датчик активен. Слежу за качеством воздуха.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)   // user cannot swipe it away
            .setSilent(true)    // no sound / vibration for the persistent notification
            .build()
    }

    private fun updateForegroundNotification(danger: Boolean) {
        val notification = if (danger) {
            NotificationCompat.Builder(this, AndroidNotificationService.CHANNEL_ID)
                .setContentTitle("Sniffer — ОПАСНОСТЬ!")
                .setContentText("Обнаружен дым или химические испарения!")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()
        } else {
            buildForegroundNotification() // your normal "всё хорошо" notification
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(FOREGROUND_NOTIF_ID, notification)
    }
}