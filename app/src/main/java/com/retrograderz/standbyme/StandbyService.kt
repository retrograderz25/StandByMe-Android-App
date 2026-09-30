package com.retrograderz.standbyme

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class StandbyService : Service() {

    private lateinit var sensorHelper: SensorHelper
    private var isCharging = false
    private var isScreenOff = false
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    checkConditionsAndStartListening()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    sensorHelper.stopListening()
                    sendStandbyStateBroadcast(false)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff = true
                    checkConditionsAndStartListening()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff = false
                }
            }
        }
    }

    private fun checkConditionsAndStartListening() {
        if (isCharging && isScreenOff) {
            sensorHelper.startListening()
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Kiểm tra trạng thái sạc hiện tại ngay khi khởi tạo service
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        sensorHelper = SensorHelper(this)
        sensorHelper.onStandbyStateChanged = { isStandbyValid ->
            if (isStandbyValid && isCharging) {
                // Đủ điều kiện: Đang sạc + Máy nghiêng đúng góc -> Mở UI Standby
                val intent = Intent(this, StandbyActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            } else if (!isStandbyValid) {
                // Sai điều kiện: Gửi broadcast để đóng Activity
                sendStandbyStateBroadcast(false)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        
        startForeground(1, createNotification())
    }

    private fun sendStandbyStateBroadcast(isValid: Boolean) {
        val intent = Intent("com.retrograderz.standbyme.STANDBY_STATE")
        intent.putExtra("isValid", isValid)
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification {
        val channelId = "standby_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Standby Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Standby đang hoạt động")
            .setContentText("Lắng nghe trạng thái sạc và màn hình")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        sensorHelper.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
