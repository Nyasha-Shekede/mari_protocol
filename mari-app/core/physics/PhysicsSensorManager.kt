package com.Mari.mobileapp.core.physics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Manages all physics-based sensors and data collection for Mari protocol
 */
class PhysicsSensorManager(
    private val context: Context,
    private val sensorManager: SensorManager
) {
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // State flows for sensor data
    private val _motionData = MutableStateFlow(MotionVector(0f, 0f, 0f))
    val motionData: StateFlow<MotionVector> = _motionData

    private val _lightLevel = MutableStateFlow(0f)
    val lightLevel: StateFlow<Float> = _lightLevel

    private val _locationGrid = MutableStateFlow("")
    val locationGrid: StateFlow<String> = _locationGrid

    private val _gyroscopeData = MutableStateFlow(GyroscopeVector(0f, 0f, 0f))
    val gyroscopeData: StateFlow<GyroscopeVector> = _gyroscopeData

    private val _magneticData = MutableStateFlow(MagneticVector(0f, 0f, 0f))
    val magneticData: StateFlow<MagneticVector> = _magneticData

    private val _deviceTemperature = MutableStateFlow(0f)
    val deviceTemperature: StateFlow<Float> = _deviceTemperature

    // Sensor event listener
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        _motionData.value = MotionVector(
                            x = event.values[0],
                            y = event.values[1],
                            z = event.values[2]
                        )
                    }

                    Sensor.TYPE_LIGHT -> {
                        _lightLevel.value = event.values[0]
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        _gyroscopeData.value = GyroscopeVector(
                            x = event.values[0],
                            y = event.values[1],
                            z = event.values[2]
                        )
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        _magneticData.value = MagneticVector(
                            x = event.values[0],
                            y = event.values[1],
                            z = event.values[2]
                        )
                    }
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                        _deviceTemperature.value = event.values[0]
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not used but required by interface
        }
    }

    /**
     * Start listening to all required sensors
     */
    fun startSensors() {
        accelerometerSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        lightSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        gyroscopeSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        magneticSensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Initialize location grid
        updateLocationGrid()
    }

    /**
     * Stop listening to sensors to conserve battery
     */
    fun stopSensors() {
        sensorManager.unregisterListener(sensorEventListener)
    }

    /**
     * Simulate vein pattern detection (for demo purposes)
     * In a real implementation, this would use IR camera data
     */
    fun simulateVeinScan(): String {
        // Generate a consistent hash based on device ID and current time
        val deviceId = UUID.randomUUID().toString().hashCode()
        val currentTime = System.currentTimeMillis()
        val combinedHash = (deviceId xor currentTime.toInt()).toString(16)
        return combinedHash.padStart(8, '0').take(8)
    }

    /**
     * Generate a location grid based on approximate coordinates
     * In a real implementation, this would use precise GPS data
     */
    private fun updateLocationGrid() {
        // For demo, generate a mock grid based on a hash of approximate location
        val chars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ*"
        val grid = StringBuilder()

        // Generate an 8-character grid code
        for (i in 0 until 8) {
            val index = (Math.random() * chars.length).toInt()
            grid.append(chars[index])
        }

        _locationGrid.value = grid.toString()
    }

    /**
     * Generate a physics seed from all available sensor data
     */
    fun generatePhysicsSeed(): UInt {
        val blood = simulateVeinScan().hashCode()
        val motion = motionData.value.toPacked()
        val light = lightLevel.value.toInt()
        val grid = locationGrid.value.hashCode()
        val gyro = gyroscopeData.value.toPacked()
        val magnetic = magneticData.value.toPacked()
        val temp = deviceTemperature.value.toInt()

        val combined = (blood shl 24) or
               (motion shl 16) or
               (light shl 8) or
               (grid and 0xFF) or
               (gyro shl 20) or
               (magnetic shl 12) or
               (temp shl 4)
        return combined.toUInt()
    }

    /**
     * Calculate device movement intensity
     */
    fun calculateMovementIntensity(): Float {
        val motion = motionData.value
        val gyro = gyroscopeData.value

        val accelMagnitude = Math.sqrt((motion.x * motion.x + motion.y * motion.y + motion.z * motion.z).toDouble()).toFloat()
        val gyroMagnitude = Math.sqrt((gyro.x * gyro.x + gyro.y * gyro.y + gyro.z * gyro.z).toDouble()).toFloat()

        return accelMagnitude + gyroMagnitude * 0.5f
    }

    /**
     * Data class representing motion vector
     */
    data class MotionVector(val x: Float, val y: Float, val z: Float) {
        fun toPacked(): Int {
            return ((x * 100).toInt() shl 16) or
                   ((y * 100).toInt() shl 8) or
                   ((z * 100).toInt() and 0xFF)
        }
    }

    /**
     * Data class representing gyroscope vector
     */
    data class GyroscopeVector(val x: Float, val y: Float, val z: Float) {
        fun toPacked(): Int {
            return ((x * 1000).toInt() shl 16) or
                   ((y * 1000).toInt() shl 8) or
                   ((z * 1000).toInt() and 0xFF)
        }
    }

    /**
     * Data class representing magnetic field vector
     */
    data class MagneticVector(val x: Float, val y: Float, val z: Float) {
        fun toPacked(): Int {
            return ((x * 100).toInt() shl 16) or
                   ((y * 100).toInt() shl 8) or
                   ((z * 100).toInt() and 0xFF)
        }
    }
}
