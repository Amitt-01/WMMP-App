package com.example.weather.network

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.BuildConfig
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MapViewModel : ViewModel() {
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val _destinationLocation = MutableStateFlow<LatLng?>(null)
    val destinationLocation: StateFlow<LatLng?> = _destinationLocation

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isRouting = MutableStateFlow(false)
    val isRouting: StateFlow<Boolean> = _isRouting

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _nextInstruction = MutableStateFlow("Follow the route")
    val nextInstruction: StateFlow<String> = _nextInstruction

    private var searchJob: Job? = null

    fun updateCurrentLocation(lat: Double, lng: Double) {
        _currentLocation.value = LatLng(lat, lng)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun fetchSuggestions(query: String, context: Context) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 4)
                if (!addresses.isNullOrEmpty()) {
                    val formattedList = addresses.mapNotNull { address ->
                        val parts = listOfNotNull(address.featureName, address.locality, address.adminArea)
                        if (parts.isNotEmpty()) parts.joinToString(", ") else null
                    }.distinct()
                    _suggestions.value = formattedList
                } else {
                    _suggestions.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _suggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun calculateRoadRoute(context: Context, sourceText: String, destText: String, currentLoc: LatLng?) {
        if (destText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                val startLatLng: LatLng? = if (sourceText.equals("Your location", ignoreCase = true) || sourceText.isBlank()) {
                    currentLoc
                } else {
                    @Suppress("DEPRECATION")
                    val srcAddress = geocoder.getFromLocationName(sourceText, 1)
                    if (!srcAddress.isNullOrEmpty()) LatLng(srcAddress[0].latitude, srcAddress[0].longitude) else null
                }

                @Suppress("DEPRECATION")
                val destAddress = geocoder.getFromLocationName(destText, 1)
                val endLatLng: LatLng? = if (!destAddress.isNullOrEmpty()) {
                    LatLng(destAddress[0].latitude, destAddress[0].longitude)
                } else null

                if (startLatLng != null && endLatLng != null) {
                    _destinationLocation.value = endLatLng
                    fetchOSRMRoute(startLatLng, endLatLng)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchOSRMRoute(start: LatLng, end: LatLng) {
        try {
            //  New FIX:  URL BuildConfig
            val urlString = "${BuildConfig.OSRM_BASE_URL}${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson&steps=true"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "WeatherRideApp/1.0")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")

                    val points = mutableListOf<LatLng>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
                    }

                    try {
                        val legs = route.getJSONArray("legs")
                        if (legs.length() > 0) {
                            val steps = legs.getJSONObject(0).getJSONArray("steps")
                            if (steps.length() > 1) {
                                val nextStep = steps.getJSONObject(1) // 0 is 'depart'
                                val maneuver = nextStep.getJSONObject("maneuver")
                                val modifier = maneuver.optString("modifier", "")
                                val name = nextStep.optString("name", "")

                                var instruction = if (modifier.isNotEmpty()) "Turn $modifier" else "Head straight"
                                if (name.isNotEmpty()) instruction += " onto $name"

                                _nextInstruction.value = instruction.replaceFirstChar { it.uppercase() }
                            } else {
                                _nextInstruction.value = "Head straight to destination"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _nextInstruction.value = "Follow the highlighted route"
                    }

                    _routePoints.value = points
                    _isRouting.value = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun searchArea(context: Context) {
        calculateRoadRoute(context, "Your location", _searchQuery.value, _currentLocation.value)
    }

    fun clearRoute() {
        _destinationLocation.value = null
        _isRouting.value = false
        _routePoints.value = emptyList()
        _nextInstruction.value = "Follow the route"
    }
}