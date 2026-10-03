package com.example.weather.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.BuildConfig
import com.example.weather.network.MapViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(mapViewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val currentLocation by mapViewModel.currentLocation.collectAsState()
    val destinationLocation by mapViewModel.destinationLocation.collectAsState()
    val searchQuery by mapViewModel.searchQuery.collectAsState()
    val isRouting by mapViewModel.isRouting.collectAsState()
    val suggestions by mapViewModel.suggestions.collectAsState()
    val routePoints by mapViewModel.routePoints.collectAsState()
    val nextInstruction by mapViewModel.nextInstruction.collectAsState()

    var hasCenteredOnUser by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(true) }
    var isRoutingMode by remember { mutableStateOf(false) }
    var sourceQuery by remember { mutableStateOf("Your location") }
    var activeField by remember { mutableStateOf("DEST") }

    var isRideActive by remember { mutableStateOf(false) }

    val mapView = remember { MapView(context) }

    val darkStyleJson = """
    {
      "version": 8,
      "sources": {
        "google-raster": {
          "type": "raster",
          "tiles": [
            "${BuildConfig.MAP_TILE_URL}"
          ],
          "tileSize": 256,
          "attribution": "Map data © Google"
        }
      },
      "layers": [
        {
          "id": "google-raster-layer",
          "type": "raster",
          "source": "google-raster",
          "minzoom": 0,
          "maxzoom": 22,
          "paint": {
            "raster-brightness-min": 0.05,
            "raster-brightness-max": 0.45, 
            "raster-saturation": -0.7,     
            "raster-contrast": 0.45         
          }
        }
      ]
    }
    """.trimIndent()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    mapViewModel.updateCurrentLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            if (!hasCenteredOnUser && destinationLocation == null) {
                mapView.getMapAsync { map ->
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.0))
                    hasCenteredOnUser = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {

        val currentRoutePoints = routePoints
        val currentIsRouting = isRouting
        val currentDest = destinationLocation
        val currentLoc = currentLocation
        val currentSearchQuery = searchQuery

        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setStyle(Style.Builder().fromJson(darkStyleJson)) { style ->
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                val locationComponent = map.locationComponent
                                locationComponent.activateLocationComponent(
                                    LocationComponentActivationOptions.builder(context, style).build()
                                )
                                locationComponent.isLocationComponentEnabled = true
                                locationComponent.renderMode = RenderMode.COMPASS
                            }
                        }

                        map.setMaxZoomPreference(20.5)
                        map.setMinZoomPreference(3.0)
                        map.uiSettings.isCompassEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isAttributionEnabled = false

                        map.addOnMapClickListener {
                            focusManager.clearFocus()
                            if(!isRoutingMode && !isRideActive) {
                                isSearchBarVisible = !isSearchBarVisible
                            }
                            mapViewModel.clearSuggestions()
                            false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    map.clear()

                    currentLoc?.let {
                        if(sourceQuery != "Your location" && !isRideActive) {
                            map.addMarker(MarkerOptions().position(it).title("Source"))
                        }
                    }

                    currentDest?.let {
                        map.addMarker(MarkerOptions().position(it).title(currentSearchQuery))
                    }

                    if (currentIsRouting && currentRoutePoints.isNotEmpty()) {
                        map.addPolyline(PolylineOptions()
                            .addAll(currentRoutePoints)
                            .color(android.graphics.Color.parseColor("#4285F4"))
                            .width(6f)
                        )

                        if (isRideActive) {
                            val locationComponent = map.locationComponent
                            locationComponent.renderMode = RenderMode.COMPASS
                            locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
                            locationComponent.zoomWhileTracking(18.0)
                            locationComponent.tiltWhileTracking(60.0)
                        } else {
                            val locationComponent = map.locationComponent
                            locationComponent.renderMode = RenderMode.COMPASS
                            locationComponent.cameraMode = CameraMode.NONE

                            try {
                                val bounds = LatLngBounds.Builder().includes(currentRoutePoints).build()
                                map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1000)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        )

        AnimatedVisibility(
            visible = (isSearchBarVisible || isRoutingMode) || isRideActive,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                if (isRideActive) {
                    MapCardStyle(containerColor = Color(0xFF198754), border = null, elevation = 8.dp) { // 🟢 OPTIMIZED
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Filled.Directions, contentDescription = "Turn", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = nextInstruction,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else if (isRoutingMode) {
                    MapCardStyle { //OPTIMIZED (Uses default Dark Grey + Border)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            IconButton(onClick = {
                                isRoutingMode = false
                                mapViewModel.updateSearchQuery("")
                                sourceQuery = "Your location"
                                mapViewModel.clearRoute()
                                currentLocation?.let { loc ->
                                    mapView.getMapAsync { map ->
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.0))
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = sourceQuery,
                                    onValueChange = {
                                        sourceQuery = it
                                        activeField = "SOURCE"
                                        if (it.length > 2 && it != "Your location") mapViewModel.fetchSuggestions(it, context) else mapViewModel.clearSuggestions()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).onFocusChanged { if (it.isFocused) activeField = "SOURCE" },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color(0xFF64B5F6), unfocusedTextColor = Color(0xFF64B5F6)
                                    ),
                                    singleLine = true,
                                    leadingIcon = { Text("🔵", fontSize = 12.sp) }
                                )

                                HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        mapViewModel.updateSearchQuery(it)
                                        activeField = "DEST"
                                        if (it.length > 2) mapViewModel.fetchSuggestions(it, context) else mapViewModel.clearSuggestions()
                                    },
                                    placeholder = { Text("Choose destination...", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).onFocusChanged { if (it.isFocused) activeField = "DEST" },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true,
                                    leadingIcon = { Text("🔴", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            focusManager.clearFocus()
                                            mapViewModel.clearSuggestions()
                                            mapViewModel.calculateRoadRoute(context, sourceQuery, searchQuery, currentLocation)
                                        }
                                    )
                                )
                            }

                            IconButton(onClick = {
                                val temp = searchQuery
                                mapViewModel.updateSearchQuery(sourceQuery)
                                sourceQuery = temp
                            }) {
                                Icon(Icons.Filled.SwapVert, contentDescription = "Swap", tint = Color.White)
                            }
                        }
                    }
                }
                else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                            .padding(horizontal = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                mapViewModel.updateSearchQuery(it)
                                activeField = "DEST"
                                if (it.length > 2) mapViewModel.fetchSuggestions(it, context) else mapViewModel.clearSuggestions()
                            },
                            placeholder = { Text("Search area or destination...", color = Color.Gray) },
                            modifier = Modifier.weight(1f).onFocusChanged { if(it.isFocused) activeField = "DEST" },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    isSearchBarVisible = false
                                    mapViewModel.clearSuggestions()
                                    mapViewModel.searchArea(context)
                                }
                            )
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                mapViewModel.updateSearchQuery("")
                                mapViewModel.clearRoute()
                                currentLocation?.let { loc ->
                                    mapView.getMapAsync { map ->
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.0))
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                isSearchBarVisible = false
                                mapViewModel.searchArea(context)
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
                            }
                        }
                    }
                }

                if (suggestions.isNotEmpty() && !isRideActive) {
                    MapCardStyle(modifier = Modifier.padding(top = 8.dp)) { // OPTIMIZED
                        Column {
                            suggestions.forEachIndexed { index, suggestion ->
                                Text(
                                    text = suggestion,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (activeField == "SOURCE") sourceQuery = suggestion else mapViewModel.updateSearchQuery(suggestion)
                                            mapViewModel.clearSuggestions()

                                            if (sourceQuery.isNotBlank() && searchQuery.isNotBlank() && activeField == "DEST") {
                                                focusManager.clearFocus()
                                                mapViewModel.calculateRoadRoute(context, sourceQuery, searchQuery, currentLocation)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                )
                                if (index < suggestions.size - 1) {
                                    HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTTOM FLOATING BUTTONS ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (isRideActive) {
                FloatingActionButton(
                    onClick = {
                        isRideActive = false
                        mapView.getMapAsync { map ->
                            val locationComponent = map.locationComponent
                            locationComponent.cameraMode = CameraMode.NONE
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder().tilt(0.0).zoom(14.0).build()
                            ), 1000)
                        }
                    },
                    containerColor = Color(0xFFDC3545),
                    contentColor = Color.White
                ) {
                    Text(text = "END", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                }
            } else {
                if (isRoutingMode && destinationLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            val loc = destinationLocation!!
                            val uri = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out this route: $uri")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Location"))
                        },
                        containerColor = Color(0xFF3B1E54),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }
                }

                FloatingActionButton(
                    onClick = {
                        mapView.getMapAsync { map ->
                            if (isRideActive) {
                                val locationComponent = map.locationComponent
                                locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
                                locationComponent.zoomWhileTracking(18.0)
                                locationComponent.tiltWhileTracking(60.0)
                            } else {
                                currentLocation?.let { loc ->
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.0))
                                }
                            }
                        }
                    },
                    containerColor = Color(0xFF3B1E54),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("📍", fontSize = 20.sp)
                }

                if (isRoutingMode && isRouting && sourceQuery.equals("Your location", ignoreCase = true)) {
                    FloatingActionButton(
                        onClick = {
                            focusManager.clearFocus()
                            isRideActive = true
                        },
                        containerColor = Color(0xFF0D6EFD),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "RIDE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
                else if (!isRoutingMode) {
                    FloatingActionButton(
                        onClick = {
                            isRoutingMode = true
                            if (searchQuery.isNotBlank()) {
                                focusManager.clearFocus()
                                mapViewModel.clearSuggestions()
                                mapViewModel.calculateRoadRoute(context, sourceQuery, searchQuery, currentLocation)
                            }
                        },
                        containerColor = Color(0xFFA07BFF),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "GO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// New fix: Reusable Card Component for UI Consistency & Shorter Code
@Composable
fun MapCardStyle(
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF1E1E1E),
    border: BorderStroke? = BorderStroke(1.dp, Color(0x33FFFFFF)),
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}