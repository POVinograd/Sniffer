package com.example.sniffer

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.sniffer.presentation.Navigation
import com.example.sniffer.presentation.SensorMonitorService
import com.example.sniffer.ui.theme.SnifferTheme

class MainActivity : ComponentActivity() {
    // Register the permission launcher
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Без разрешения на уведомления недоступно оповещение об опасных ситуациях!.",
                    Toast.LENGTH_LONG
                ).show()
            }
            else{
                SensorMonitorService.start(this)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        else{
            SensorMonitorService.start(this)
        }
        enableEdgeToEdge()
        setContent {
            SnifferTheme {
                Navigation(
                    onLogout = {
                        Log.d("MYSERVICE", "onLogout in MainActivity called")
                        SensorMonitorService.stop(this) }
                )
            }
        }
    }
}



