package com.example.weather.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.WeatherState
import com.example.weather.WeatherViewModel
import com.google.android.gms.location.LocationServices

@Composable
fun HomeScreen(viewModel: WeatherViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val weatherState by viewModel.weatherState.collectAsState()

    var tempDisplay = "--°C"
    var windDisplay = "-- km/h"
    var cityDisplay = "Locating..."
    var districtDisplay = "Locating..."
    var stateDisplay = "State"

    when (weatherState) {
        is WeatherState.Success -> {
            val data = weatherState as WeatherState.Success
            tempDisplay = data.temperature
            windDisplay = data.windSpeed
            cityDisplay = data.cityName
            districtDisplay = data.districtName
            stateDisplay = data.stateName
        }
        is WeatherState.Loading -> {
            tempDisplay = "..."
            windDisplay = "..."
            cityDisplay = "Loading..."
            districtDisplay = "Loading..."
            stateDisplay = "Loading..."
        }
        is WeatherState.Error -> {
            tempDisplay = "Error"
            windDisplay = "Error"
            cityDisplay = "Error"
            districtDisplay = "Error"
            stateDisplay = "Error"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.fetchWeather(location.latitude, location.longitude, context)
                    } else {
                        viewModel.fetchWeather(28.4744, 77.5040, context)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            viewModel.fetchWeather(28.4744, 77.5040, context)
        }
    }

    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.fetchWeather(location.latitude, location.longitude, context)
                } else {
                    viewModel.fetchWeather(28.4744, 77.5040, context)
                }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp)
                .verticalScroll(scrollState)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // FIX: Screenshot ke according yahan se aas pas ke icons hata diye gaye hain
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cityDisplay, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("Mostly Sunny", color = Color.LightGray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = "Weather Icon",
                tint = Color(0xFFF9D25D),
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x26FFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(vertical = 20.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetailItem(Icons.Outlined.WaterDrop, "30%", "Precipitation")
                WeatherDetailItem(Icons.Outlined.WaterDrop, tempDisplay, "Temp")
                WeatherDetailItem(Icons.Outlined.Air, windDisplay, "Wind Speed")
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Others", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(2) { index ->
                    val title = if (index == 0) districtDisplay else stateDisplay
                    val desc = if (index == 0) "Sunny" else "Cloudy"
                    val temp = if (index == 0) tempDisplay else "24°C"
                    CityCard(title, desc, temp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text("Hourly", color = Color.LightGray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val times = listOf("10 AM", "12 AM", "1 PM", "3 PM", "5 PM")
                items(times.size) { i ->
                    HourlyCard(times[i], tempDisplay)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("7-Day Forecasts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val daysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val conditions = listOf("Sunny", "Cloudy", "Thunder", "Thunder", "Rain", "Rain", "Sunny")
            val temps = listOf("+31°", "+40°", "+33°", "+27°", "+39°", "+44°", "+35°")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x26FFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (i in daysList.indices) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = daysList[i],
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )

                        Row(
                            modifier = Modifier.weight(1.2f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = conditions[i],
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = temps[i],
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // PATLI SI BLUR LINE
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .fillMaxWidth()
                .height(16.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF121212)
                        )
                    )
                )
        )
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
fun CityCard(city: String, description: String, temp: String) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x26FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(temp, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HourlyCard(time: String, temp: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x26FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(time, color = Color.LightGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(temp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}