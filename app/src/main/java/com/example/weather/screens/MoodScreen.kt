package com.example.weather.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.weather.WeatherState
import com.example.weather.WeatherViewModel
import com.example.weather.network.MusicViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@SuppressLint("DefaultLocale")
fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun MoodScreen(
    weatherViewModel: WeatherViewModel = viewModel(),
    musicViewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentView = LocalView.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentSpeed by remember { mutableStateOf(0f) }
    val maxSpeed = 120f

    val weatherState by weatherViewModel.weatherState.collectAsState()
    var tempDisplay = "--°C"
    var windDisplay = "--km/h"
    var weatherCondition = "Clear"

    if (weatherState is WeatherState.Success) {
        val data = weatherState as WeatherState.Success
        tempDisplay = data.temperature
        windDisplay = data.windSpeed.replace(" ", "")
        weatherCondition = data.condition
    }

    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentPos by musicViewModel.currentPosition.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    val songTitle by musicViewModel.currentTitle.collectAsState()
    val songArtist by musicViewModel.currentArtist.collectAsState()
    val songThumbnail by musicViewModel.currentThumbnail.collectAsState()

    var sliderPosition by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentPos) {
        if (!isDragging) {
            sliderPosition = currentPos.toFloat()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                musicViewModel.searchYouTube(matches[0])
            }
        }
    }

    LaunchedEffect(weatherCondition) {
        musicViewModel.autoSearchByWeather(weatherCondition)
    }

    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    if (location.hasSpeed()) {
                        val speedKmh = location.speed * 3.6f
                        currentSpeed = if (speedKmh < 5.0f) 0f else speedKmh
                    } else {
                        currentSpeed = 0f
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).setMinUpdateIntervalMillis(1000).build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) weatherViewModel.fetchWeather(location.latitude, location.longitude, context)
                }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF3B1E54), Color(0xFF121212)))
            )
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bollywood Vibes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(weatherCondition.uppercase(), color = Color.LightGray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f), 0f)
                drawArc(Color(0x4DFFFFFF), 180f, 180f, false, style = Stroke(width = strokeWidth, pathEffect = dashEffect))
                val currentSpeedProgress = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
                drawArc(Color.White, 180f, 180f * currentSpeedProgress, false, style = Stroke(width = strokeWidth, pathEffect = dashEffect))
            }

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(Color(0x33000000))
            ) {
                if (songThumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = songThumbnail,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .align(Alignment.TopCenter)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter).background(Color(0xFF1A1A1A)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("${currentSpeed.toInt()}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("km/h", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
                Icon(Icons.Filled.Cloud, contentDescription = "Weather", tint = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(tempDisplay, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                Text(
                    text = songTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = songArtist,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp)) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Search",
                    tint = Color(0xFFA07BFF),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Konsa gaana sunna hai?")
                            }
                            speechLauncher.launch(intent)
                        }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Icon(Icons.Outlined.Air, contentDescription = "Air", tint = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(windDisplay, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x26FFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            val maxRange = if (duration > 0) duration.toFloat() else 1f
            Slider(
                value = sliderPosition,
                onValueChange = {
                    isDragging = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    musicViewModel.seekTo(sliderPosition.toLong())
                },
                valueRange = 0f..maxRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFA07BFF),
                    inactiveTrackColor = Color(0x4DFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPos), color = Color.LightGray, fontSize = 12.sp)
                Text(formatTime(duration), color = Color.LightGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // New FIX: Refresh Button
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { musicViewModel.refreshSongs() }
                )

                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp).clickable { musicViewModel.playPrevious() }
                )

                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp).clickable { musicViewModel.togglePlayPause() }
                )

                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp).clickable { musicViewModel.playNext() }
                )

                Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
        }
    }
}