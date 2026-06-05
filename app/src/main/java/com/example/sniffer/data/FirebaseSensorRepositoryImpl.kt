package com.example.sniffer.data

import com.example.sniffer.domain.SensorData
import com.example.sniffer.domain.SensorRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class FirebaseSensorRepositoryImpl : SensorRepository {
    // Слушаем конкретно один узел "bme680"
    private val databaseRef = FirebaseDatabase.getInstance().getReference("bme680")

    override fun getSensorStream(): Flow<List<SensorData>> = callbackFlow {
        // Локальный список в памяти телефона для накопления точек графика НАДО ЗАМЕНИТЬ, ДАННЫЕ НЕ СОХРАНЯЮТСЯ
        val historyList = mutableListOf<SensorData>()

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Извлекаем одно текущее измерение из Firebase
                val temp = snapshot.child("temperature").getValue(Float::class.java) ?: 0f
                val hum = snapshot.child("humidity").getValue(Float::class.java) ?: 0f
                val press = snapshot.child("pressure").getValue(Float::class.java) ?: 0f
                val gas = snapshot.child("gas_resistance").getValue(Float::class.java) ?: 0f
                val time = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                val currentReading = SensorData(time, temp, hum, press, gas)

                // Добавляем новое измерение к уже существующим точкам графика
                historyList.add(currentReading)

                // Чтобы график не перегружался, храним только последние 20 точек
                if (historyList.size > 20) {
                    historyList.removeAt(0)
                }

                // Отправляем всю накопленную историю в Jetpack Compose
                trySend(historyList.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        databaseRef.addValueEventListener(listener)

        // Очищаем слушатель при уничтожении экрана
        awaitClose { databaseRef.removeEventListener(listener) }
    }
}