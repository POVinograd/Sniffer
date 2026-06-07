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

    // Локальный список в памяти телефона для накопления точек графика
    private val historyList = mutableListOf<SensorData>()

    companion object {
        private const val MAX_HISTORY = 20 //максимальное количество сохранённых данных
    }

    override fun getSensorStream(): Flow<List<SensorData>> = callbackFlow {

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Извлекаем одно текущее измерение из Firebase
                val temp = snapshot.child("temperature").getValue(Float::class.java) ?: 0f
                val hum = snapshot.child("humidity").getValue(Float::class.java) ?: 0f
                val press = snapshot.child("pressure").getValue(Float::class.java) ?: 0f
                val gas = snapshot.child("iaq").getValue(Float::class.java) ?: 0f
                val time = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                val currentReading = SensorData(time, temp, hum, press, gas)

                // Добавляем новое измерение к уже существующим точкам графика
                if (historyList.lastOrNull()?.timestamp != time) {
                    historyList.add(currentReading)
                    if (historyList.size > MAX_HISTORY) {
                        historyList.removeAt(0)
                    }
                }

                // Отправляем всю накопленную историю в Jetpack Compose
                trySend(historyList.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        databaseRef.addValueEventListener(listener)

        // очистить слушатель при уничтожении экрана
        awaitClose { databaseRef.removeEventListener(listener) }
    }
}