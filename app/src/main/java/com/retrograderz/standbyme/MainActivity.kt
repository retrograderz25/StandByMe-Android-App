package com.retrograderz.standbyme

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.retrograderz.standbyme.ui.theme.StandByMeTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startStandbyServiceInternal()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            StandByMeTheme {
                MainScreen(
                    checkOverlay = { hasOverlayPermission() },
                    checkNotification = { hasNotificationPermission() },
                    onRequestOverlay = { requestOverlayPermission() },
                    onRequestNotification = { requestNotificationPermission() },
                    onStartService = { startStandbyService() }
                )
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun hasNotificationPermission(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun startStandbyService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startStandbyServiceInternal()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startStandbyServiceInternal()
        }
    }

    private fun startStandbyServiceInternal() {
        val intent = Intent(this, StandbyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun MainScreen(
    checkOverlay: () -> Boolean,
    checkNotification: () -> Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onStartService: () -> Unit
) {
    var hasOverlay by remember { mutableStateOf(checkOverlay()) }
    var hasNotification by remember { mutableStateOf(checkNotification()) }
    
    val lifecycleOwner = LocalLifecycleOwner.current

    // Tự động kiểm tra lại quyền khi người dùng từ Settings quay lại App
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = checkOverlay()
                hasNotification = checkNotification()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cấu hình StandByMe", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onRequestOverlay) {
                Text(if (hasOverlay) "✅ Đã cấp quyền Overlay" else "❌ Yêu cầu quyền Overlay")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onRequestNotification) {
                Text(if (hasNotification) "✅ Đã cấp quyền Notification" else "❌ Yêu cầu quyền Notification")
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onStartService,
                enabled = hasOverlay && hasNotification,
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Text("Khởi động Standby Service")
            }
        }
    }
}