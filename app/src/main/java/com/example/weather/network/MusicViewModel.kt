package com.example.weather.network

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.weather.BuildConfig // 🟢 NAYA FIX: BuildConfig Import kiya
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// AUDIUS API MODELS
data class AudiusResponse(val data: List<AudiusTrack>?)
data class AudiusTrack(val id: String, val title: String?, val user: AudiusUser?, val artwork: AudiusArtwork?)
data class AudiusUser(val name: String?)
data class AudiusArtwork(
    @SerializedName("150x150") val small: String?,
    @SerializedName("480x480") val medium: String?,
    @SerializedName("1000x1000") val large: String?
)

interface AudiusApiService {
    @GET("v1/tracks/search")
    suspend fun searchTracks(@Query("query") query: String, @Query("app_name") appName: String = "WeatherMoodApp"): AudiusResponse
}

object AudiusRetrofitClient {
    val apiService: AudiusApiService by lazy {
        Retrofit.Builder()
            // 🟢 NAYA FIX: Hardcoded URL ki jagah BuildConfig lagaya
            .baseUrl(BuildConfig.AUDIUS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudiusApiService::class.java)
    }
}

// VIEWMODEL START
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration
    private val _currentTitle = MutableStateFlow("Ready to Play")
    val currentTitle: StateFlow<String> = _currentTitle
    private val _currentArtist = MutableStateFlow("...")
    val currentArtist: StateFlow<String> = _currentArtist
    private val _currentThumbnail = MutableStateFlow("")
    val currentThumbnail: StateFlow<String> = _currentThumbnail

    private var lastWeatherCondition = ""

    init {
        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(240000, 300000, 2500, 5000).build()
        exoPlayer = ExoPlayer.Builder(application).setLoadControl(loadControl).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val dur = this@apply.duration
                        if (dur > 0) _duration.value = dur
                    }
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentPosition.value = 0L
                    val dur = this@apply.duration
                    if (dur > 0) _duration.value = dur
                    mediaItem?.mediaMetadata?.let { meta ->
                        _currentTitle.value = meta.title?.toString() ?: "Unknown Song"
                        _currentArtist.value = meta.artist?.toString() ?: "Unknown Artist"
                        _currentThumbnail.value = meta.artworkUri?.toString() ?: ""
                    }
                }
            })
        }
        startProgressUpdate()
    }

    // New: Refresh Button
    fun refreshSongs() {
        if (lastWeatherCondition.isEmpty()) lastWeatherCondition = "Clear"
        fetchSongsForWeather(lastWeatherCondition)
    }

    fun autoSearchByWeather(weatherCondition: String) {
        if (weatherCondition == lastWeatherCondition && exoPlayer?.mediaItemCount ?: 0 > 0) return
        lastWeatherCondition = weatherCondition
        fetchSongsForWeather(weatherCondition)
    }

    private fun fetchSongsForWeather(weatherCondition: String) {
        val autoQuery = when {
            weatherCondition.contains("Rain", ignoreCase = true) -> "Bollywood Lofi"
            weatherCondition.contains("Fog", ignoreCase = true) || weatherCondition.contains("Mist", ignoreCase = true) -> "Hindi Love Song"
            else -> "Hindi 90s"
        }
        searchYouTube(autoQuery)
    }

    fun searchYouTube(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _currentTitle.value = "Searching Premium Tracks..."
                val response = AudiusRetrofitClient.apiService.searchTracks(query = query)

                if (response.data != null && response.data.isNotEmpty()) {
                    val forbiddenWords = listOf("party", "club", "edm", "trap", "item", "bhojpuri", "mashup dance", "dj bass", "rap", "english", "hip hop", "hiphop", "phonk", "type beat", "drill", "metal")

                    val premiumFilteredList = response.data.filter { track ->
                        val title = track.title?.lowercase() ?: ""
                        val artist = track.user?.name?.lowercase() ?: ""
                        !forbiddenWords.any { word -> title.contains(word) || artist.contains(word) }
                    }

                    val finalPlaylist = if (premiumFilteredList.isNotEmpty()) premiumFilteredList.shuffled() else response.data.shuffled()

                    launch(Dispatchers.Main) { playPlaylist(finalPlaylist) }
                } else {
                    launch(Dispatchers.Main) { _currentTitle.value = "No results found" }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "API Error: ${e.message}")
                launch(Dispatchers.Main) { _currentTitle.value = "Search Failed" }
            }
        }
    }

    private fun playPlaylist(tracks: List<AudiusTrack>) {
        exoPlayer?.let { player ->
            player.clearMediaItems()
            val mediaItems = tracks.map { track ->
                val imageUrl = track.artwork?.large ?: track.artwork?.medium ?: ""
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title ?: "Unknown Track")
                    .setArtist(track.user?.name ?: "Unknown Artist")
                    .setArtworkUri(Uri.parse(imageUrl))
                    .build()

                // 🟢 NAYA FIX: Hardcoded URL ki jagah BuildConfig lagaya
                val streamUrl = "${BuildConfig.AUDIUS_BASE_URL}v1/tracks/${track.id}/stream?app_name=WeatherMoodApp"

                MediaItem.Builder().setUri(streamUrl).setMediaMetadata(metadata).build()
            }
            player.setMediaItems(mediaItems)
            player.prepare()
            player.play()
        }
    }

    fun playNext() { exoPlayer?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
    fun playPrevious() { exoPlayer?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() } }
    fun togglePlayPause() { exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(position: Long) { exoPlayer?.seekTo(position); _currentPosition.value = position }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                        if (it.duration > 0) _duration.value = it.duration
                    }
                }
                delay(1000L)
            }
        }
    }

    override fun onCleared() { super.onCleared(); exoPlayer?.release() }
}