package com.example.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weather.ui.theme.WeatherTheme

// AuthViewModel aur AuthStep ko import kiya hai
import com.example.weather.network.AuthStep
import com.example.weather.network.AuthViewModel

// Sabhi screens ko import kiya hai
import com.example.weather.screens.HomeScreen
import com.example.weather.screens.MoodScreen
import com.example.weather.screens.RideScreen
import com.example.weather.screens.ProfileScreen
import com.example.weather.screens.AuthScreen // Sign In screen ka import

import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        enableEdgeToEdge()
        setContent {
            WeatherTheme(darkTheme = true) {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("home", Icons.Filled.Home)
    object Mood : Screen("mood", Icons.Filled.MusicNote)
    object Ride : Screen("ride", Icons.Filled.Map)
    object Profile : Screen("profile", Icons.Filled.Person)
}

@Composable
fun MainScreen(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2B2D33), Color(0xFF121212))
                )
            )
    ) {

        NavHost(navController = navController, startDestination = Screen.Home.route) {

            // 1. HOME (Weather) - Ye sabke liye open hai
            composable(Screen.Home.route) {
                HomeScreen()
            }

            // 2. MOOD (Music) - Guarded by ProtectedScreenWrapper
            composable(Screen.Mood.route) {
                ProtectedScreenWrapper(authViewModel) {
                    MoodScreen()
                }
            }

            // 3. RIDE (Map) - Guarded by ProtectedScreenWrapper
            composable(Screen.Ride.route) {
                ProtectedScreenWrapper(authViewModel) {
                    RideScreen()
                }
            }

            // 4. PROFILE - Guarded by ProtectedScreenWrapper
            composable(Screen.Profile.route) {
                ProtectedScreenWrapper(authViewModel) {
                    ProfileScreen(authViewModel)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            FloatingBottomNavigationBar(navController = navController)
        }
    }
}

//  New FIX:wrapper check
@Composable
fun ProtectedScreenWrapper(authViewModel: AuthViewModel, content: @Composable () -> Unit) {
    val user by authViewModel.user.collectAsState()
    val authStep by authViewModel.authStep.collectAsState()

    val isAuthenticated = user != null && authStep != AuthStep.SETUP_PROFILE

    // State to toggle between the friendly message and the actual AuthScreen
    var showSignInPage by remember { mutableStateOf(false) }

    if (isAuthenticated) {
        // Agar login hai toh seedha feature dikhao
        content()
    } else {
        if (showSignInPage) {
            // Button click karne ke baad asli Sign In screen khulegi
            AuthScreen(authViewModel)
        } else {
            // Pyara sa message bina login wale users ke liye
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x26FFFFFF)) // Glassmorphism effect
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFA07BFF), // Premium purple color
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Unlock Premium Features!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Please sign in to enjoy Music, Navigation, and manage your Profile. It's completely free!",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { showSignInPage = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA07BFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Sign In Now",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigationBar(navController: NavHostController) {
    val screens = listOf(Screen.Home, Screen.Mood, Screen.Ride, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(30.dp))
            .padding(vertical = 8.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0x4DFFFFFF) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}