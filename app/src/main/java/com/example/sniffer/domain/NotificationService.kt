package com.example.sniffer.domain

interface NotificationService {
    fun createChannel()
    fun sendSmokeAlert(iaq: Int, temperature: Int)
}