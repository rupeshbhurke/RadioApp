package com.example.radioapp.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.example.radioapp.model.Station
import com.example.radioapp.service.RadioPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RadioController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentStation = MutableStateFlow<Station?>(null)
    val currentStation: StateFlow<Station?> = _currentStation.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, RadioPlayerService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) _errorMessage.value = null
                    }
                    
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // Media transition handled by play()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _errorMessage.value = "Error playing station: ${error.message}"
                        _isPlaying.value = false
                    }
                })
            },
            MoreExecutors.directExecutor()
        )
    }

    fun play(station: Station) {
        _errorMessage.value = null
        _currentStation.value = station
        
        val remainingMinutes = _sleepTimerRemaining.value?.let { (it / 60000).toInt() + 1 }
        val subtitle = if (remainingMinutes != null && remainingMinutes > 0) {
            "${station.city} • \uD83D\uDE34 ${remainingMinutes}m left"
        } else {
            station.city
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(subtitle)
                    .apply {
                        if (station.logoUrl.isNotBlank()) {
                            setArtworkUri(android.net.Uri.parse(station.logoUrl))
                        }
                    }
                    .build()
            )
            .build()
            
        mediaController?.let {
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }

    fun pause() {
        mediaController?.pause()
    }
    
    fun resume() {
        mediaController?.play()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining: StateFlow<Long?> = _sleepTimerRemaining.asStateFlow()

    private val _sleepTimerExpired = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val sleepTimerExpired = _sleepTimerExpired.asSharedFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    private fun updateMediaMetadataWithTimer(remainingMinutes: Int?) {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem ?: return
        val station = _currentStation.value ?: return

        val subtitle = if (remainingMinutes != null && remainingMinutes > 0) {
            "${station.city} • \uD83D\uDE34 ${remainingMinutes}m left"
        } else {
            station.city
        }

        val newMetadata = currentItem.mediaMetadata.buildUpon()
            .setArtist(subtitle)
            .build()

        val newItem = currentItem.buildUpon()
            .setMediaMetadata(newMetadata)
            .build()

        controller.replaceMediaItem(controller.currentMediaItemIndex, newItem)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = null
            updateMediaMetadataWithTimer(null)
            return
        }

        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        sleepTimerJob = kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            var lastRemainingMinutes = -1
            while (true) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _sleepTimerRemaining.value = null
                    updateMediaMetadataWithTimer(null)
                    pause()
                    _sleepTimerExpired.emit(Unit)
                    break
                }
                _sleepTimerRemaining.value = remaining
                
                val remainingMinutes = (remaining / 60000).toInt() + 1
                if (remainingMinutes != lastRemainingMinutes) {
                    lastRemainingMinutes = remainingMinutes
                    updateMediaMetadataWithTimer(remainingMinutes)
                }
                
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    fun release() {
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
