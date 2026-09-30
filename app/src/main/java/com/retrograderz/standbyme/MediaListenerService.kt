package com.retrograderz.standbyme

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaInfo(
    val trackName: String? = null,
    val artistName: String? = null,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false
)

class MediaListenerService : NotificationListenerService() {

    companion object {
        private val _mediaInfoFlow = MutableStateFlow(MediaInfo())
        val mediaInfoFlow: StateFlow<MediaInfo> = _mediaInfoFlow.asStateFlow()
        
        private var activeController: MediaController? = null

        fun play() { activeController?.transportControls?.play() }
        fun pause() { activeController?.transportControls?.pause() }
        fun skipToNext() { activeController?.transportControls?.skipToNext() }
        fun skipToPrevious() { activeController?.transportControls?.skipToPrevious() }
    }

    private lateinit var mediaSessionManager: MediaSessionManager
    
    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers?.firstOrNull())
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(MediaSessionManager::class.java)
        
        val componentName = ComponentName(this, MediaListenerService::class.java)
        try {
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            updateActiveController(controllers.firstOrNull())
            mediaSessionManager.addOnActiveSessionsChangedListener(activeSessionsChangedListener, componentName)
        } catch (e: SecurityException) {
            // Xử lý khi chưa được cấp quyền truy cập thông báo
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
            activeController?.unregisterCallback(controllerCallback)
        } catch (e: Exception) {}
    }

    private fun updateActiveController(controller: MediaController?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        activeController?.registerCallback(controllerCallback)
        
        // Cập nhật thông tin hiện tại
        activeController?.let {
            updateMediaInfo(it.metadata, it.playbackState)
        } ?: run {
            _mediaInfoFlow.value = MediaInfo()
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo(metadata, activeController?.playbackState)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo(activeController?.metadata, state)
        }
    }

    private fun updateMediaInfo(metadata: MediaMetadata?, state: PlaybackState?) {
        val track = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        
        // Lấy ảnh bìa nhạc từ metadata
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        
        _mediaInfoFlow.value = MediaInfo(
            trackName = track,
            artistName = artist,
            albumArt = art,
            isPlaying = isPlaying
        )
    }
}
