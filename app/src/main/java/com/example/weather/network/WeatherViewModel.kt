package com.example.weather

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(
        val temperature: String,
        val windSpeed: String,
        val cityName: String, // Area Name
        val districtName: String,
        val stateName: String,
        val condition: String = "Clear" // remove red error
    ) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState

    fun fetchWeather(lat: Double, lon: Double, context: Context) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val response = RetrofitClient.apiService.getWeather(lat = lat, lon = lon)

                val temp = "${response.current_weather.temperature}°C"
                val wind = "${response.current_weather.windspeed} km/h"

                // Default weather condition (Agar API me weather condition/code aaye toh yahan parse kar sakte hain)
                val condition = "Clear"

                var areaName = "Area"
                var district = "District"
                var state = "State"

                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lon, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]

                        // 1. Area Name
                        areaName = address.subLocality ?: address.locality ?: address.subAdminArea ?: "Unknown Area"

                        // 2. District Name
                        var tempDistrict = address.subAdminArea ?: address.locality ?: "Gautam Buddha Nagar"
                        if (tempDistrict.contains("Meerut", ignoreCase = true) || tempDistrict.contains("Division", ignoreCase = true)) {
                            tempDistrict = "Gautam Buddha Nagar"
                        }
                        district = tempDistrict

                        // 3. State Name
                        state = address.adminArea ?: "Uttar Pradesh"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Geocoder fail fallback
                    areaName = "Greater Noida"
                    district = "Gautam Buddha Nagar"
                    state = "Uttar Pradesh"
                }

                // condition pass
                _weatherState.value = WeatherState.Success(temp, wind, areaName, district, state, condition)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error("Network Error")
            }
        }
    }
}