package com.example.weather.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.network.AuthStep
import com.example.weather.network.AuthViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(authViewModel: AuthViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val user by authViewModel.user.collectAsState()
    val profile by authViewModel.userProfile.collectAsState()
    val authStep by authViewModel.authStep.collectAsState()

    val settingsLoading by authViewModel.settingsLoading.collectAsState()
    val settingsError by authViewModel.settingsError.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var nameErrorMsg by remember { mutableStateOf("") }

    // SETTINGS STATE VARIABLES
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showDeleteWarning by remember { mutableStateOf(false) }
    var showChangePwdDialog by remember { mutableStateOf(false) }
    var showSignOutWarning by remember { mutableStateOf(false) }

    // SECURITY POPUPS
    var showDobVerification by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var showNewPasswordDialog by remember { mutableStateOf(false) }

    // ANIMATION STATES
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

    val isUpdateAvailable by remember { mutableStateOf(true) }

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
                        Icon(Icons.Filled.Dashboard, contentDescription = "Logo", tint = Color.White)
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

                if (isUpdateAvailable) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF198754).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF198754), RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri("https://github.com/your-repo/releases") } // Apna GitHub release link daal dein
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = "Update", tint = Color(0xFF4BB543), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("New Update Available", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap here to download the latest version.", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("General", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

                MenuItem(icon = Icons.Outlined.Info, title = "About Developer", onClick = { uriHandler.openUri("https://amitt-m.vercel.app/") })
                MenuItem(icon = Icons.Outlined.Article, title = "Documentation", onClick = { uriHandler.openUri("https://github.com/your-username/your-repo#readme") })

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

    //  DIALOGS (POPUPS) SECTION
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
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
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
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
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
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPwdInput, onValueChange = { confirmPwdInput = it },
                        label = { Text("Re-enter New Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
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
                                } else pwdChangeError = msg
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
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
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
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPwdInput, onValueChange = { confirmPwdInput = it },
                        label = { Text("Re-enter New Password", color = Color.Gray) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF222222), focusedIndicatorColor = Color(0xFFA07BFF), unfocusedIndicatorColor = Color(0xFF333333))
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

fun formatDobDisplay(dob: String?): String {
    if (dob.isNullOrEmpty()) return "Not set yet"
    return try {
        val parts = dob.split("/")
        if (parts.size == 3) {
            val day = parts[0]
            val monthIndex = parts[1].toInt() - 1
            val year = parts[2]
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = months.getOrElse(monthIndex) { "" }
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
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = months.getOrElse(monthIndex) { "" }
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