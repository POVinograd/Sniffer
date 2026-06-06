package com.example.sniffer.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sniffer.data.FirebaseSensorRepositoryImpl
import com.example.sniffer.data.FirebaseUserRepositoryImpl
import com.example.sniffer.domain.SensorData
import com.example.sniffer.domain.SensorRepository
import com.example.sniffer.domain.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class MetricType { TEMPERATURE, HUMIDITY, PRESSURE, GAS }

class MainViewModel : ViewModel() {
    private val sensorRepository: SensorRepository = FirebaseSensorRepositoryImpl()
    private val userRepository:   UserRepository   = FirebaseUserRepositoryImpl()
    val sensorData: StateFlow<List<SensorData>> = sensorRepository.getSensorStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedMetric by mutableStateOf(MetricType.TEMPERATURE)

    fun logout(){
        userRepository.logout()
    }
}