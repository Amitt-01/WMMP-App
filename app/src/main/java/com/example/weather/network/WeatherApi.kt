package com.example.weather.network

import com.example.weather.BuildConfig // 🟢 NAYA FIX: BuildConfig import kiya
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Data Classes
data class WeatherResponse(
    val current_weather: CurrentWeather
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double
)

// 2. API Interface
interface WeatherApiService {
    @GET("forecast?current_weather=true")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double
    ): WeatherResponse
}

// 3. Retrofit Instance
object RetrofitClient {
    val apiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}