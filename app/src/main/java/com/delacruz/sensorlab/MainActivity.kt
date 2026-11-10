package com.delacruz.sensorlab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.delacruz.sensorlab.ui.theme.SensorLabTheme
import kotlin.math.abs
import kotlin.math.sqrt

/** Gyroscope magnitude (rad/s) above which the device is considered "moving". */
const val GYRO_MOVEMENT_THRESHOLD = 0.5f

/** Static hardware details read from a [Sensor] (Part 18 extension). */
data class SensorDetails(
    val name: String,
    val vendor: String,
    val maximumRange: Float,
    val resolution: Float
)

private fun Sensor.toDetails() = SensorDetails(
    name = name,
    vendor = vendor,
    maximumRange = maximumRange,
    resolution = resolution
)

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    // getDefaultSensor() returns null when the hardware is missing, so these are nullable.
    private var temperatureSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null

    // Compose-observable state
    private var temperature by mutableStateOf<Float?>(null)
    private var gyroX by mutableStateOf(0f)
    private var gyroY by mutableStateOf(0f)
    private var gyroZ by mutableStateOf(0f)

    private var temperatureAvailable by mutableStateOf(false)
    private var gyroscopeAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager

        temperatureSensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        gyroscopeSensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        temperatureAvailable = temperatureSensor != null
        gyroscopeAvailable = gyroscopeSensor != null

        val temperatureDetails = temperatureSensor?.toDetails()
        val gyroscopeDetails = gyroscopeSensor?.toDetails()

        setContent {
            SensorLabTheme {
                SensorScreen(
                    temperature = temperature,
                    temperatureAvailable = temperatureAvailable,
                    gyroX = gyroX,
                    gyroY = gyroY,
                    gyroZ = gyroZ,
                    gyroscopeAvailable = gyroscopeAvailable,
                    temperatureDetails = temperatureDetails,
                    gyroscopeDetails = gyroscopeDetails
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        temperatureSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        gyroscopeSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                val celsius = event.values[0]
                // Some sensors (e.g. the emulator) emit a placeholder like -1e30 before a
                // real reading exists; ignore values outside the sensor's reported range.
                if (celsius.isFinite() && abs(celsius) <= event.sensor.maximumRange) {
                    temperature = celsius
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0] // rad/s around X
                gyroY = event.values[1] // rad/s around Y
                gyroZ = event.values[2] // rad/s around Z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required for this lab.
    }
}

@Composable
fun SensorScreen(
    temperature: Float?,
    temperatureAvailable: Boolean,
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    gyroscopeAvailable: Boolean,
    temperatureDetails: SensorDetails? = null,
    gyroscopeDetails: SensorDetails? = null
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Android Sensor Monitor",
                style = MaterialTheme.typography.headlineMedium
            )

            TemperatureCard(
                temperature = temperature,
                available = temperatureAvailable,
                details = temperatureDetails
            )

            GyroscopeCard(
                x = gyroX,
                y = gyroY,
                z = gyroZ,
                available = gyroscopeAvailable,
                details = gyroscopeDetails
            )
        }
    }
}

@Composable
fun TemperatureCard(
    temperature: Float?,
    available: Boolean,
    details: SensorDetails? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ambient Temperature",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                !available ->
                    Text("Temperature sensor is not available.")

                temperature == null ->
                    Text("Waiting for sensor reading...")

                else ->
                    Text(
                        text = "%.1f °C".format(temperature),
                        style = MaterialTheme.typography.headlineSmall
                    )
            }

            if (details != null) {
                SensorDetailsSection(details = details, unit = "°C")
            }
        }
    }
}

@Composable
fun GyroscopeCard(
    x: Float,
    y: Float,
    z: Float,
    available: Boolean,
    details: SensorDetails? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gyroscope",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!available) {
                Text("Gyroscope is not available.")
            } else {
                Text("X: %.3f rad/s".format(x))
                Text("Y: %.3f rad/s".format(y))
                Text("Z: %.3f rad/s".format(z))

                Spacer(modifier = Modifier.height(8.dp))
                MovementIndicator(x = x, y = y, z = z)

                if (details != null) {
                    SensorDetailsSection(details = details, unit = "rad/s")
                }
            }
        }
    }
}

/** Part 18 extension: visual indicator when gyroscope movement exceeds a threshold. */
@Composable
fun MovementIndicator(x: Float, y: Float, z: Float) {
    val magnitude = sqrt(x * x + y * y + z * z)
    val moving = magnitude > GYRO_MOVEMENT_THRESHOLD

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (moving) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (moving) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    ) {
        Text(
            text = if (moving) {
                "ROTATING (%.2f rad/s)".format(magnitude)
            } else {
                "Still (%.2f rad/s)".format(magnitude)
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/** Part 18 extension: sensor name, vendor, maximum range and resolution. */
@Composable
fun SensorDetailsSection(details: SensorDetails, unit: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Sensor details",
        style = MaterialTheme.typography.labelLarge
    )
    Text("Name: ${details.name}", style = MaterialTheme.typography.bodySmall)
    Text("Vendor: ${details.vendor}", style = MaterialTheme.typography.bodySmall)
    Text(
        "Max range: %.3f %s".format(details.maximumRange, unit),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "Resolution: %.5f %s".format(details.resolution, unit),
        style = MaterialTheme.typography.bodySmall
    )
}

@Preview(showBackground = true)
@Composable
fun SensorScreenPreview() {
    SensorLabTheme {
        SensorScreen(
            temperature = null,
            temperatureAvailable = false,
            gyroX = 0.012f,
            gyroY = -0.004f,
            gyroZ = 0.731f,
            gyroscopeAvailable = true,
            gyroscopeDetails = SensorDetails(
                name = "Preview Gyroscope",
                vendor = "Preview Vendor",
                maximumRange = 34.906f,
                resolution = 0.00107f
            )
        )
    }
}
