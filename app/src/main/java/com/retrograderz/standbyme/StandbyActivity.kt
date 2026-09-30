package com.retrograderz.standbyme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.retrograderz.standbyme.ui.theme.StandByMeTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.retrograderz.standbyme.ui.StandbyScreen

class StandbyActivity : ComponentActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isValid = intent?.getBooleanExtra("isValid", true) ?: true
            if (!isValid) {
                // Nhận được lệnh kết thúc từ Service (do sai góc hoặc rút sạc)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Thiết lập các cờ cho Activity hiển thị đè lên màn hình khóa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Ẩn System UI để FullScreen hoàn toàn
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Đăng ký BroadcastReceiver để lắng nghe lệnh đóng từ Service
        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter("com.retrograderz.standbyme.STANDBY_STATE"),
            ContextCompat.RECEIVER_EXPORTED
        )

        setContent {
            StandByMeTheme {
                val mediaInfo by MediaListenerService.mediaInfoFlow.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StandbyScreen(mediaInfo = mediaInfo)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)
    }
}
