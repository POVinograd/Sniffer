package com.example.sniffer.domain

object AirQualityAdvisor {
    //Пороговые значения
    private const val IAQ_STAGNANT = 100f   //загрязненный воздух
    private const val HUMIDITY_MOLD = 65f    //риск плесени
    private const val HUMIDITY_DRY = 30f    //сухой воздух

    private const val IAQ_DANGER = 180f
    private const val TEMP_SMOKE_SPIKE = 30f    //повышение температуры
    private const val IAQ_SPIKE_DELTA = 80f    //скачок относительно прошлого измерения

    data class Alert(
        val id: String,
        val title: String,
        val message: String,
        val severity: Severity
    )

    enum class Severity { INFO, WARNING, DANGER }

    /**
     * @param current  the latest sensor reading
     * @param previous the reading just before [current], or null if unavailable
     */
    fun evaluate(current: SensorData, previous: SensorData?): List<Alert> {
        val alerts = mutableListOf<Alert>()

        //Сценарий 1: застоявшийся воздух
        if (current.iaq > IAQ_STAGNANT) {
            alerts += Alert(
                id = "stagnant_air",
                title = "Застоявшийся воздух",
                message = "Индекс качества воздуха составляет ${current.iaq.toInt()}. " +
                        "Откройте окно на 10-15 мин для проветривания комнаты",
                severity = Severity.WARNING
            )
        }

        //Сценарий 2: опасность образования плесени
        if (current.humidity > HUMIDITY_MOLD) {
            alerts += Alert(
                id = "mold_risk",
                title = "Риск развития плесени",
                message = "Влажность  ${current.humidity.toInt()}% — более 65% способствует " +
                        "образованию плесени и грибка. Что насчёт осушителя воздуха?.",
                severity = Severity.WARNING
            )
        }

        //Сценарий 3: слишком сухой воздух
        if (current.humidity < HUMIDITY_DRY) {
            alerts += Alert(
                id = "dry_air",
                title = "Сухой воздух",
                message = "Влажность ${current.humidity.toInt()}% — ниже 30% раздражает " +
                        "кожу и слизистые. Что насчёт увлажнителя воздуха?.",
                severity = Severity.INFO
            )
        }

        //Сценарий 4: дым / сгоревшая еда / газ
        val iaqJump = previous?.let { current.iaq - it.iaq } ?: 0f
        val smokeDetected = current.iaq > IAQ_DANGER &&
                current.temperature > 28f &&
                iaqJump > IAQ_SPIKE_DELTA
        if (smokeDetected) {
            alerts += Alert(
                id = "smoke",
                title = "Дым / утечка газа!",
                message = "Высокий показатель качества воздуха ${current.iaq.toInt()} вместе с " +
                        "температурой (${current.temperature.toInt()}°C)." +
                        "Проверьте помещение на наличие дыма / сгоревшей еды / утечки газа немедленно!",
                severity = Severity.DANGER
            )
        }

        return alerts.sortedByDescending { it.severity.ordinal }
    }


    // Функция возвращает ИСТИНА, если показания соответствуют опасности дыма / хим. испарений
    fun isSmokeAlert(current: SensorData, previous: SensorData?): Boolean {
        val iaqJump = previous?.let { current.iaq - it.iaq } ?: 0f
        return current.iaq > IAQ_DANGER &&
                current.temperature > TEMP_SMOKE_SPIKE &&
                iaqJump > IAQ_SPIKE_DELTA
    }
}