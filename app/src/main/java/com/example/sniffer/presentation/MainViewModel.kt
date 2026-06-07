package com.example.sniffer.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sniffer.data.AndroidNotificationService
import com.example.sniffer.data.FirebaseSensorRepositoryImpl
import com.example.sniffer.data.FirebaseUserRepositoryImpl
import com.example.sniffer.domain.AirQualityAdvisor
import com.example.sniffer.domain.NotificationService
import com.example.sniffer.domain.SensorData
import com.example.sniffer.domain.SensorRepository
import com.example.sniffer.domain.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

enum class MetricType { TEMPERATURE, HUMIDITY, PRESSURE, GAS }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorRepository: SensorRepository = FirebaseSensorRepositoryImpl()
    private val userRepository:   UserRepository   = FirebaseUserRepositoryImpl()

    private val notifications: NotificationService = AndroidNotificationService.also {
        it.init(app)
        it.createChannel()
    }
    val sensorData: StateFlow<List<SensorData>> = sensorRepository.getSensorStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedMetric by mutableStateOf(MetricType.TEMPERATURE)

    var activeAlerts by mutableStateOf<List<AirQualityAdvisor.Alert>>(emptyList())
        private set

    init {
        observeAlerts()
    }

    private fun observeAlerts() {
        sensorData.onEach { history ->
            if (history.isEmpty()) return@onEach
            val current  = history.last()
            val previous = if (history.size >= 2) history[history.size - 2] else null

            activeAlerts = AirQualityAdvisor.evaluate(current, previous)

            // Fire system notification for smoke / danger scenario
            if (AirQualityAdvisor.isSmokeAlert(current, previous)) {
                notifications.sendSmokeAlert(
                    iaq         = current.iaq.toInt(),
                    temperature = current.temperature.toInt()
                )
            }
        }.launchIn(viewModelScope)
    }
    fun logout(){
        userRepository.logout()
    }
}