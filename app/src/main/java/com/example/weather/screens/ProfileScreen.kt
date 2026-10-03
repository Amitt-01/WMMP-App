package com.example.weather.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.BuildConfig
import com.example.weather.R
import com.example.weather.network.AuthStep
import com.example.weather.network.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(authViewModel: AuthViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val user by authViewModel.user.collectAsState()
    val profile by authViewModel.userProfile.collectAsState()
    val authStep by authViewModel.authStep.collectAsState()

    val settingsLoading by authViewModel.settingsLoading.collectAsState()
    val settingsError by authViewModel.settingsError.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var nameErrorMsg by remember { mutableStateOf("") }

    var showSettingsMenu by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showChangePwdDialog by remember { mutableStateOf(false) }
    var showSignOutWarning by remember { mutableStateOf(false) }

    var showDobVerification by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var showNewPasswordDialog by remember { mutableStateOf(false) }

    // State for About Dialog
    var showAboutDialog by remember { mutableStateOf(false) }

    var showSuccessAnimation by remember { mutableStateOf(false) }
    var successAnimMessage by remember { mutableStateOf("") }

    var currentAction by remember { mutableStateOf("") }

    var oldPwdInput by remember { mutableStateOf("") }
    var newPwdInput by remember { mutableStateOf("") }
    var confirmPwdInput by remember { mutableStateOf("") }
    var pwdChangeError by remember { mutableStateOf("") }

    var dobInput by remember { mutableStateOf("") }
    var dobErrorMsg by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isUpdateAvailable by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) } // Newly added state variable
    var updateUrl by remember { mutableStateOf("") }

    // Fetch app version dynamically from build.gradle
    val currentAppVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1"
    } catch (e: Exception) {
        "1.1" // Default to 1.1 if an error occurs
    }

    fun checkForUpdate() {
        coroutineScope.launch(Dispatchers.IO) {
            isCheckingUpdate = true
            try {
                val url = URL(BuildConfig.UPDATE_CHECK_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val latestVersion = jsonObject.getString("latestVersion")
                    val apkUrl = jsonObject.getString("downloadUrl")

                    if (latestVersion != currentAppVersion) {
                        isUpdateAvailable = true
                        updateUrl = apkUrl
                    } else {
                        isUpdateAvailable = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdate()
    }

    if (user == null || authStep == AuthStep.SETUP_PROFILE) {
        AuthScreen(authViewModel)
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D)).statusBarsPadding().padding(bottom = 90.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // Profile Header
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                    .background(Color(0xFF865D46)).padding(top = 16.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        // Logo Updated Here
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WMMP", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.DarkGray, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val displayName = profile?.fullName ?: user!!.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"
                    val displaySubtitle = user!!.email ?: "User Account"
                    Text(displayName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(displaySubtitle, color = Color(0xFFE5D5CB), fontSize = 14.sp)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("General", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                    if (isUpdateAvailable) {
                        val infiniteTransition = rememberInfiniteTransition(label = "bell_transition")
                        val angle by infiniteTransition.animateFloat(
                            initialValue = -20f,
                            targetValue = 20f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(150, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bell_animation"
                        )

                        // Replaced Browser download with In-App Download Function
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !isDownloadingUpdate) {
                                    isDownloadingUpdate = true
                                    downloadAndInstallUpdate(context, updateUrl) {
                                        isDownloadingUpdate = false
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = "Update Available",
                                tint = Color(0xFF198754),
                                modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = if(isDownloadingUpdate) 0f else angle }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isDownloadingUpdate) "Downloading..." else "Update App", color = Color(0xFF198754), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (isCheckingUpdate) "Checking..." else "Check Update",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { checkForUpdate() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayFullName = if (profile?.fullName.isNullOrEmpty()) "Not set yet" else profile!!.fullName!!
                val displayDob = formatDobDisplay(profile?.dob)
                val displayDoj = formatDate(profile?.doj)

                InfoItem(title = "Full Name", value = displayFullName, showEdit = true, onEditClick = { nameInput = profile?.fullName ?: ""; nameErrorMsg = ""; showNameDialog = true })
                InfoItem(title = "Date of Birth", value = displayDob, showEdit = false)
                InfoItem(title = "Date of Joining", value = displayDoj, showEdit = false)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("App Info", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))

                // Changed About link to show In-App Dialog & Updated Documentation Link
                MenuItem(icon = Icons.Outlined.Info, title = "About", onClick = { showAboutDialog = true })
                MenuItem(icon = Icons.Outlined.Article, title = "Documentation", onClick = { uriHandler.openUri("https://github.com/Amitt-01/WMMP-App.git") })

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Profile", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))

                MenuItem(icon = Icons.Outlined.Settings, title = "Setting", onClick = { showSettingsMenu = true })

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSignOutWarning = true }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Sign Out", color = Color(0xFFFF6B6B), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // DIALOGS SECTION

    // Custom About App Dialog (Matching screenshot style)
    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF151515))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Circular Logo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(45.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Version
                    Text(
                        text = "Version $currentAppVersion",
                        color = Color(0xFFA07BFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Brief App Description
                    Text(
                        text = "WMMP is a simple and secure app to access weather updates, music, and maps effortlessly.",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Developer Credit
                    Text(
                        text = "Developed by WMMP",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSignOutWarning) {
        AlertDialog(
            onDismissRequest = { showSignOutWarning = false },
            containerColor = Color(0xFF151515),
            title = { Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of your account?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                    onClick = {
                        showSignOutWarning = false
                        authViewModel.signOut()
                    }
                ) { Text("Yes, Sign Out", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showSignOutWarning = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF151515),
            title = { Text("Edit Full Name", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput, onValueChange = { if (it.length <= 15) nameInput = it },
                        label = { Text("Max 15 letters", color = Color.Gray) }, singleLine = true,
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${nameInput.length}/15", color = Color.Gray, fontSize = 12.sp)
                    if (nameErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(nameErrorMsg, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { authViewModel.updateName(nameInput) { msg, success -> if (success) showNameDialog = false else nameErrorMsg = msg } }) { Text("Save", color = Color(0xFFA07BFF), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showSettingsMenu) {
        ModalBottomSheet(onDismissRequest = { showSettingsMenu = false }, containerColor = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Account Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        showSettingsMenu = false
                        oldPwdInput = ""; newPwdInput = ""; confirmPwdInput = ""; pwdChangeError = ""
                        showChangePwdDialog = true
                    }.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Change Password", color = Color.White, fontSize = 16.sp)
                }

                HorizontalDivider(color = Color(0xFF333333))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showSettingsMenu = false; showDeleteWarning = true }.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Delete Account", color = Color(0xFFFF6B6B), fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showChangePwdDialog) {
        AlertDialog(
            onDismissRequest = { showChangePwdDialog = false },
            containerColor = Color(0xFF151515),
            title = { Text("Change Password", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPwdInput, onValueChange = { oldPwdInput = it },
                        label = { Text("Old Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "Forgot Password?", color = Color(0xFFA07BFF), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                showChangePwdDialog = false
                                currentAction = "FORGOT_PWD"
                                dobInput = ""; dobErrorMsg = ""
                                showDobVerification = true
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPwdInput, onValueChange = { newPwdInput = it },
                        label = { Text("New Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPwdInput, onValueChange = { confirmPwdInput = it },
                        label = { Text("Re-enter New Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (settingsLoading) { Spacer(modifier = Modifier.height(16.dp)); CircularProgressIndicator(color = Color(0xFFA07BFF), modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)) }
                    if (pwdChangeError.isNotEmpty()) { Spacer(modifier = Modifier.height(12.dp)); Text(pwdChangeError, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !settingsLoading && oldPwdInput.isNotBlank() && newPwdInput.isNotBlank() && confirmPwdInput.isNotBlank(),
                    onClick = {
                        if (newPwdInput.length < 6) pwdChangeError = "Password must be at least 6 characters"
                        else if (newPwdInput != confirmPwdInput) pwdChangeError = "New passwords do not match"
                        else {
                            pwdChangeError = ""
                            authViewModel.changePasswordWithOld(oldPwdInput, newPwdInput) { msg, success ->
                                if (success) {
                                    showChangePwdDialog = false
                                    successAnimMessage = "Password Changed Successfully!"
                                    showSuccessAnimation = true
                                }
                            }
                        }
                    }
                ) { Text("Save", color = Color(0xFFA07BFF), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showChangePwdDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            containerColor = Color(0xFF151515),
            title = { Text("Are you sure?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Do you really want to delete your account? This action cannot be undone.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                    onClick = {
                        showDeleteWarning = false
                        currentAction = "DELETE_ACC"
                        dobInput = ""; dobErrorMsg = ""
                        showDobVerification = true
                    }
                ) { Text("Yes, Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteWarning = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showDobVerification) {
        AlertDialog(
            onDismissRequest = { showDobVerification = false },
            containerColor = Color(0xFF151515),
            title = { Text("Security Verification", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please enter your Date of Birth to continue.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dobInput, onValueChange = { dobInput = it },
                        label = { Text("e.g. DD/MM/YYYY", color = Color.Gray) }, singleLine = true,
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (settingsLoading) { Spacer(modifier = Modifier.height(16.dp)); CircularProgressIndicator(color = Color(0xFFA07BFF), modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)) }
                    if (dobErrorMsg.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(dobErrorMsg, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                    if (!settingsError.isNullOrEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(settingsError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !settingsLoading && dobInput.isNotBlank(),
                    onClick = {
                        if (authViewModel.checkDobMatch(dobInput)) {
                            dobErrorMsg = ""
                            authViewModel.requestSettingsOtp { success ->
                                if (success) {
                                    showDobVerification = false
                                    otpInput = ""
                                    showOtpDialog = true
                                }
                            }
                        } else dobErrorMsg = "Date of Birth does not match our records."
                    }
                ) { Text("Verify", color = Color(0xFFA07BFF), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDobVerification = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            containerColor = Color(0xFF151515),
            title = { Text("Enter OTP", color = Color.White) },
            text = {
                Column {
                    Text("Enter the 6-digit OTP sent to your email.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    BasicTextField(
                        value = otpInput, onValueChange = { if (it.length <= 6) otpInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        decorationBox = {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                repeat(6) { index ->
                                    val char = if (index < otpInput.length) otpInput[index].toString() else ""
                                    Box(
                                        modifier = Modifier.size(40.dp).border(1.dp, if (otpInput.length == index) Color(0xFFA07BFF) else Color(0xFF333333), RoundedCornerShape(8.dp)).background(Color(0xFF222222)),
                                        contentAlignment = Alignment.Center
                                    ) { Text(text = char, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    )

                    if (settingsLoading) { Spacer(modifier = Modifier.height(16.dp)); CircularProgressIndicator(color = Color(0xFFA07BFF), modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)) }
                    if (!settingsError.isNullOrEmpty()) { Spacer(modifier = Modifier.height(16.dp)); Text(settingsError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = otpInput.length == 6 && !settingsLoading,
                    onClick = {
                        authViewModel.verifySettingsOtp(otpInput) { success ->
                            if (success) {
                                showOtpDialog = false
                                otpInput = ""
                                if (currentAction == "FORGOT_PWD") {
                                    newPwdInput = ""; confirmPwdInput = ""; pwdChangeError = ""
                                    showNewPasswordDialog = true
                                } else if (currentAction == "DELETE_ACC") {
                                    authViewModel.deleteAccountForever { deleted ->
                                        if (deleted) {
                                            successAnimMessage = "Account Deleted Permanently!"
                                            showSuccessAnimation = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { Text("Verify", color = Color(0xFFA07BFF), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showOtpDialog = false; otpInput = "" }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showNewPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showNewPasswordDialog = false },
            containerColor = Color(0xFF151515),
            title = { Text("Set New Password", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPwdInput, onValueChange = { newPwdInput = it },
                        label = { Text("New Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPwdInput, onValueChange = { confirmPwdInput = it },
                        label = { Text("Re-enter New Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (settingsLoading) { Spacer(modifier = Modifier.height(16.dp)); CircularProgressIndicator(color = Color(0xFFA07BFF), modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)) }
                    if (pwdChangeError.isNotEmpty()) { Spacer(modifier = Modifier.height(12.dp)); Text(pwdChangeError, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                    if (!settingsError.isNullOrEmpty()) { Spacer(modifier = Modifier.height(16.dp)); Text(settingsError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newPwdInput.length >= 6 && confirmPwdInput.isNotBlank() && !settingsLoading,
                    onClick = {
                        if (newPwdInput != confirmPwdInput) pwdChangeError = "Passwords do not match."
                        else {
                            pwdChangeError = ""
                            authViewModel.updatePasswordFromSettings(newPwdInput) { success ->
                                if (success) {
                                    showNewPasswordDialog = false
                                    successAnimMessage = "Password Changed Successfully!"
                                    showSuccessAnimation = true
                                }
                            }
                        }
                    }
                ) { Text("Update", color = Color(0xFFA07BFF), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showNewPasswordDialog = false; newPwdInput = "" }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showSuccessAnimation) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(visible = true, enter = scaleIn(animationSpec = tween(500))) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = Color(0xFF198754), modifier = Modifier.size(72.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(successAnimMessage, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        LaunchedEffect(Unit) {
            delay(2500)
            showSuccessAnimation = false
            if (currentAction == "DELETE_ACC") {
                authViewModel.setAuthStep(AuthStep.LOGIN)
                authViewModel.signOut()
            }
        }
    }
}

// In-App Download Code added at the bottom
fun downloadAndInstallUpdate(context: Context, apkUrl: String, onCompleteCallback: () -> Unit) {
    Toast.makeText(context, "Update downloading in background...", Toast.LENGTH_SHORT).show()

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val uri = Uri.parse(apkUrl)
    val request = DownloadManager.Request(uri).apply {
        setTitle("WMMP Update")
        setDescription("Downloading latest version...")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "WMMP_Update.apk")
    }

    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WMMP_Update.apk")
    if (file.exists()) file.delete()

    val downloadId = downloadManager.enqueue(request)

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                try {
                    val apkUri = FileProvider.getUriForFile(ctxt, "${ctxt.packageName}.provider", file)
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctxt.startActivity(installIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(ctxt, "Failed to start installation.", Toast.LENGTH_SHORT).show()
                }
                onCompleteCallback()
                ctxt.unregisterReceiver(this)
            }
        }
    }

    ContextCompat.registerReceiver(
        context,
        onComplete,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        ContextCompat.RECEIVER_EXPORTED
    )
}

@Composable
fun dialogTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222),
    focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333)
)

private val MONTHS_LIST = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatDobDisplay(dob: String?): String {
    if (dob.isNullOrEmpty()) return "Not set yet"
    return try {
        val parts = dob.split("/")
        if (parts.size == 3) {
            val day = parts[0]
            val monthIndex = parts[1].toInt() - 1
            val year = parts[2]
            val monthName = MONTHS_LIST.getOrElse(monthIndex) { "" }
            "$day $monthName $year"
        } else dob
    } catch (e: Exception) { "Format Error" }
}

fun formatDate(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "Not set yet"
    return try {
        val datePart = dateString.substringBefore("T").substringBefore(" ")
        val parts = datePart.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthIndex = parts[1].toInt() - 1
            val day = parts[2]
            val monthName = MONTHS_LIST.getOrElse(monthIndex) { "" }
            "$day $monthName $year"
        } else dateString
    } catch (e: Exception) { "Format Error" }
}

@Composable
fun MenuItem(icon: ImageVector, title: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun InfoItem(title: String, value: String, showEdit: Boolean = false, onEditClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF151515)).border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = if (value == "Not set yet") Color(0xFF666666) else Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        if (showEdit) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onEditClick() }.border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text(text = "Edit", color = Color(0xFFA07BFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}