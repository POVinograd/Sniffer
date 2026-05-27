package com.example.sniffer.domain
import com.example.sniffer.domain.SensorData
import kotlinx.coroutines.flow.Flow


interface SensorRepository {
    fun getSensorStream(): Flow<List<SensorData>>
}