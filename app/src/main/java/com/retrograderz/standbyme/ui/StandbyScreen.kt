package com.retrograderz.standbyme.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrograderz.standbyme.MediaInfo
import com.retrograderz.standbyme.MediaListenerService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
fun StandbyScreen(mediaInfo: MediaInfo) {
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }

    // Logic chống Burn-in (Burn-in Protection Logic)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L) // Cứ mỗi 60 giây
            offsetX = Random.nextInt(-15, 15).dp
            offsetY = Random.nextInt(-15, 15).dp
        }
    }

    val animatedOffsetX by animateDpAsState(targetValue = offsetX, animationSpec = tween(1000), label = "offsetX")
    val animatedOffsetY by animateDpAsState(targetValue = offsetY, animationSpec = tween(1000), label = "offsetY")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedOffsetX, y = animatedOffsetY),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cột Trái (Thời gian & Ngày tháng)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                ClockComponent()
            }

            // Cột Phải (Media Player)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                MediaComponent(mediaInfo)
            }
        }
    }
}

@Composable
fun ClockComponent() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))

    val timeString = timeFormat.format(Date(currentTime))
    val dateString = dateFormat.format(Date(currentTime))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeString,
            color = Color(0xFFE0E0E0),
            fontSize = 96.sp, // Kích thước font lớn, vừa phải với landscape
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            maxLines = 1
        )
        Text(
            text = dateString,
            color = Color(0xFFAAAAAA),
            fontSize = 24.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun MediaComponent(mediaInfo: MediaInfo) {
    if (mediaInfo.trackName.isNullOrEmpty() && mediaInfo.artistName.isNullOrEmpty() && mediaInfo.albumArt == null) {
        // Nếu không có nhạc
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow, // Tạm dùng PlayArrow nếu ko có MusicNote core
                contentDescription = "No Media",
                modifier = Modifier
                    .size(80.dp)
                    .alpha(0.3f),
                tint = Color.White
            )
        }
    } else {
        // Nếu có nhạc
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Ảnh bìa nhạc
            if (mediaInfo.albumArt != null) {
                Image(
                    bitmap = mediaInfo.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Default Album Art",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tên bài hát
            Text(
                text = mediaInfo.trackName ?: "Unknown Track",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tên ca sĩ
            Text(
                text = mediaInfo.artistName ?: "Unknown Artist",
                color = Color.Gray,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Các nút điều khiển
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { MediaListenerService.skipToPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = {
                    if (mediaInfo.isPlaying) MediaListenerService.pause()
                    else MediaListenerService.play()
                }) {
                    Icon(
                        imageVector = if (mediaInfo.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }

                IconButton(onClick = { MediaListenerService.skipToNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
