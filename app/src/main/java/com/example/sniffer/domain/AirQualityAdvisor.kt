package com.example.sniffer.domain

object AirQualityAdvisor {
    //Пороговые значения
    private const val IAQ_STAGNANT = 150f   //загрязненный воздух
    private const val HUMIDITY_MOLD = 65f    //риск плесени
    private const val HUMIDITY_DRY = 30f    //сухой воздух
    private const val IAQ_SMOKE_SPIKE = 100f   //резкий скачок IAQ
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
                title = "Stagnant air detected",
                message = "Индекс качества воздуха составляет ${current.iaq.toInt()} — качество воздуха ухудшилось. " +
                        "Откройте окно на 10-15 мин для проветривания комнаты",
                severity = Severity.WARNING
            )
        }

        //Сценарий 2: опасность образования плесени
        if (current.humidity > HUMIDITY_MOLD) {
            alerts += Alert(
                id = "mold_risk",
                title = "Mold risk",
                message = "Влажность  ${current.humidity.toInt()}% — более 65% способствует " +
                        "образованию плесени и грибка. Что насчёт осушителя воздуха?.",
                severity = Severity.WARNING
            )
        }

        //Сценарий 3: слишком сухой воздух
        if (current.humidity < HUMIDITY_DRY) {
            alerts += Alert(
                id = "dry_air",
                title = "Air is too dry",
                message = "Влажность ${current.humidity.toInt()}% — ниже 30% раздражает " +
                        "кожу и слизистые. Что насчёт увлажнителя воздуха?.",
                severity = Severity.INFO
            )
        }

        //Сценарий 4: дым / сгоревшая еда / химия
        val iaqJump = previous?.let { current.iaq - it.iaq } ?: 0f
        val smokeDetected = current.iaq > IAQ_SMOKE_SPIKE &&
                current.temperature > 28f/*TEMP_SMOKE_SPIKE*/ &&
                iaqJump > IAQ_SPIKE_DELTA
        if (smokeDetected) {
            alerts += Alert(
                id = "smoke",
                title = "Smoke / chemical vapour alert!",
                message = "Резкий скачок индекса качества воздуха (${current.iaq.toInt()}, +" +
                        "${iaqJump.toInt()} за одно измерение) вместе с высокой " +
                        "температурой (${current.temperature.toInt()}°C)." +
                        "Проверьте помещение на наличие дыма / сгоревшей еды / хим. испарений немедленно!",
                severity = Severity.DANGER
            )
        }

        return alerts.sortedByDescending { it.severity.ordinal }
    }


    // Функция возвращает ИСТИНА, если показания соответствуют опасности дыма / хим. испарений
    fun isSmokeAlert(current: SensorData, previous: SensorData?): Boolean {
        val iaqJump = previous?.let { current.iaq - it.iaq } ?: 0f
        return current.iaq > IAQ_SMOKE_SPIKE &&
                current.temperature > TEMP_SMOKE_SPIKE &&
                iaqJump > IAQ_SPIKE_DELTA
    }
}