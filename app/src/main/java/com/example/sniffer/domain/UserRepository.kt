package com.example.sniffer.domain
import com.example.sniffer.domain.SensorData
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(email: String, pass: String): Result<Unit>
    fun logout()
    fun isLoggedIn(): Boolean
}