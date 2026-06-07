package com.example.sniffer.presentation

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sniffer.domain.AirQualityAdvisor
import com.example.sniffer.domain.SensorData

@Composable
fun MainScreen(
    name: String?,
    onLogout: () -> Unit,
    viewModel: MainViewModel = viewModel()) {
    val timelineData by viewModel.sensorData.collectAsState()

    val currentReading = timelineData.lastOrNull() ?: SensorData(0L, 0f, 0f, 0f, 0f)
    val alerts = viewModel.activeAlerts

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        //Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            //Приветствие
            //Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Hello, $name", style = MaterialTheme.typography.headlineMedium)
            //Spacer(modifier = Modifier.height(16.dp))
            //Кнопка выхода из аккаунта
            IconButton(onClick = {
                Log.d("LOGOUT", "")
                viewModel.logout()
                onLogout()

            }) {
                Icon(Icons.Filled.Close, contentDescription = "Log out")
            }
        }
        //Сетка сенсорных карточек
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                SensorCard(
                    title = "Температура",
                    value = "${String.format("%.1f", currentReading.temperature)} °C",
                    isSelected = viewModel.selectedMetric == MetricType.TEMPERATURE
                ) {
                    viewModel.selectedMetric = MetricType.TEMPERATURE
                }
            }
            item {
                SensorCard(
                    title = "Влажность",
                    value = "${String.format("%.1f", currentReading.humidity)} %",
                    isSelected = viewModel.selectedMetric == MetricType.HUMIDITY
                ) {
                    viewModel.selectedMetric = MetricType.HUMIDITY
                }
            }
            item {
                SensorCard(
                    title = "Давление",
                    value = "${String.format("%.0f", currentReading.pressure)} hPa",
                    isSelected = viewModel.selectedMetric == MetricType.PRESSURE
                ) {
                    viewModel.selectedMetric = MetricType.PRESSURE
                }
            }
            item {
                SensorCard(
                    title = "Качество воздуха",
                    value = "${String.format("%.0f", currentReading.iaq)} IAQ",
                    isSelected = viewModel.selectedMetric == MetricType.GAS
                ) {
                    viewModel.selectedMetric = MetricType.GAS
                }
            }
        }
        //График
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Live Curve: ${viewModel.selectedMetric.name}",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Отрисовываем график, только если у нас есть хотя бы 1 точка данных
        if (timelineData.isNotEmpty()) {
            Bme680Graph(dataPoints = timelineData, selectedType = viewModel.selectedMetric)
        } else {
            // Если данных пока нет, заглушка
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color.DarkGray.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Text(text = "Ожидание данных из Firebase...", color = Color.Gray)
            }
        }

        //Alert-карточка
        Text(text = "Air Quality Status", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (alerts.isEmpty()) {
            AlertCard(
                title = "All good!",
                message = "Ноу проблемо. Качество воздуха в порядке.",
                severity = AirQualityAdvisor.Severity.INFO
            )
        } else {
            alerts.forEach { alert ->
                AlertCard(
                    title = alert.title,
                    message = alert.message,
                    severity = alert.severity
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SensorCard(title: String, value: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun Bme680Graph(dataPoints: List<SensorData>, selectedType: MetricType) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.DarkGray.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
    ) {
        if (dataPoints.size == 1) {
            val bmeData = dataPoints.first()
            val floatValue = when (selectedType) {
                MetricType.TEMPERATURE -> bmeData.temperature
                MetricType.HUMIDITY -> bmeData.humidity
                MetricType.PRESSURE -> bmeData.pressure
                MetricType.GAS -> bmeData.iaq
            }
            drawCircle(color = Color(0xFF6200EE), radius = 6f, center = Offset(size.width / 2, size.height / 2))
            return@Canvas
        }

        val path = Path()
        val widthBetweenPoints = size.width / (dataPoints.size - 1)
        val (minVal, maxVal) = when (selectedType) {
            MetricType.TEMPERATURE -> Pair(0f, 40f)
            MetricType.HUMIDITY -> Pair(0f, 100f)
            MetricType.PRESSURE -> Pair(950f, 1050f)
            MetricType.GAS -> Pair(0f, 500f)
        }
        val valueRange = maxVal - minVal

        dataPoints.forEachIndexed { index, bmeData ->
            val floatValue = when (selectedType) {
                MetricType.TEMPERATURE -> bmeData.temperature
                MetricType.HUMIDITY -> bmeData.humidity
                MetricType.PRESSURE -> bmeData.pressure
                MetricType.GAS -> bmeData.iaq
            }

            val clampedValue = floatValue.coerceIn(minVal, maxVal)
            val x = index * widthBetweenPoints

            // Защита от деления на ноль, если диапазон выставлен неверно
            val denominator = if (valueRange == 0f) 1f else valueRange
            val y = size.height - ((clampedValue - minVal) / denominator * size.height)

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = Color(0xFF6200EE), radius = 4f, center = Offset(x, y))
        }
        drawPath(path = path, color = Color(0xFF3700B3), style = Stroke(width = 4f))
    }
}

@Composable
fun AlertCard(
    title: String,
    message: String,
    severity: AirQualityAdvisor.Severity
) {
    val (bgColor, borderColor) = when (severity) {
        AirQualityAdvisor.Severity.DANGER  ->
            MaterialTheme.colorScheme.errorContainer  to MaterialTheme.colorScheme.error
        AirQualityAdvisor.Severity.WARNING ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) to
                    MaterialTheme.colorScheme.tertiary
        AirQualityAdvisor.Severity.INFO    ->
            MaterialTheme.colorScheme.surfaceVariant  to MaterialTheme.colorScheme.primary
    }
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}