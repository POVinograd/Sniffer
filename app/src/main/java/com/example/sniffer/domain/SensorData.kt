package com.example.sniffer.domain

data class SensorData(
    val timestamp: Long,
    val temperature: Float,
    val humidity: Float,
    val pressure: Float,
    val iaq: Float
)