package com.retrograderz.standbyme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class SensorHelper(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var isListening = false
    private var lastState = false
    
    var onStandbyStateChanged: ((Boolean) -> Unit)? = null
    
    fun startListening() {
        if (!isListening && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            isListening = true
        }
    }
    
    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
            lastState = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                
                // Khi máy nằm ngang dọc theo cạnh dài (landscape), trọng lực sẽ tác động mạnh nhất lên trục X.
                val isLandscape = abs(x) > 5f && abs(y) < 4f
                
                // Góc nghiêng so với mặt bàn (Pitch): abs(z) < 6.5 tương ứng với góc khoảng từ 50 đến 90 độ (dựng đứng)
                val isUpright = abs(z) < 6.5f
                
                val isValid = isLandscape && isUpright
                
                // Chỉ gọi callback khi trạng thái thay đổi
                if (isValid != lastState) {
                    lastState = isValid
                    onStandbyStateChanged?.invoke(isValid)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
