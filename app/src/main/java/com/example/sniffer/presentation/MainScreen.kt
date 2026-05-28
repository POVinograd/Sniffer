package com.example.sniffer.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sniffer.domain.SensorData

@Composable
fun MainScreen(name: String?, viewModel: MainViewModel = viewModel()) {
    val timelineData by viewModel.sensorData.collectAsState()
    // Безопасно берем последний элемент. Если его нет, используем дефолтные нули
    val currentReading = timelineData.lastOrNull() ?: SensorData(0L, 0f, 0f, 0f, 0f)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Hello, $name", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                SensorCard(
                    title = "Temperature",
                    value = "${String.format("%.1f", currentReading.temperature)} °C",
                    isSelected = viewModel.selectedMetric == MetricType.TEMPERATURE
                ) {
                    viewModel.selectedMetric = MetricType.TEMPERATURE
                }
            }
            item {
                SensorCard(
                    title = "Humidity",
                    value = "${String.format("%.1f", currentReading.humidity)} %",
                    isSelected = viewModel.selectedMetric == MetricType.HUMIDITY
                ) {
                    viewModel.selectedMetric = MetricType.HUMIDITY
                }
            }
            item {
                SensorCard(
                    title = "Pressure",
                    value = "${String.format("%.0f", currentReading.pressure)} hPa",
                    isSelected = viewModel.selectedMetric == MetricType.PRESSURE
                ) {
                    viewModel.selectedMetric = MetricType.PRESSURE
                }
            }
            item {
                SensorCard(
                    title = "Gas Resistance",
                    value = "${String.format("%.0f", currentReading.gasResistance / 1000f)} kΩ",
                    isSelected = viewModel.selectedMetric == MetricType.GAS
                ) {
                    viewModel.selectedMetric = MetricType.GAS
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Live Curve: ${viewModel.selectedMetric.name}", style = MaterialTheme.typography.titleMedium)
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
                    .background(Color.DarkGray.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
            ) {
                Text(text = "Waiting for Firebase data...", color = Color.Gray)
            }
        }
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
            .clickable { onClick() } // Теперь клик просто меняет вкладку без падения
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
                MetricType.GAS -> bmeData.gasResistance
            }
            drawCircle(color = Color(0xFF6200EE), radius = 6f, center = Offset(size.width / 2, size.height / 2))
            return@Canvas
        }

        val path = Path()
        val widthBetweenPoints = size.width / (dataPoints.size - 1)
        val (minVal, maxVal) = when (selectedType) {
            MetricType.TEMPERATURE -> Pair(15f, 40f)
            MetricType.HUMIDITY -> Pair(0f, 100f)
            MetricType.PRESSURE -> Pair(950f, 1050f)
            MetricType.GAS -> Pair(0f, 150000f)
        }
        val valueRange = maxVal - minVal

        dataPoints.forEachIndexed { index, bmeData ->
            val floatValue = when (selectedType) {
                MetricType.TEMPERATURE -> bmeData.temperature
                MetricType.HUMIDITY -> bmeData.humidity
                MetricType.PRESSURE -> bmeData.pressure
                MetricType.GAS -> bmeData.gasResistance
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