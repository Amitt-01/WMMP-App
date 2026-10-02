package com.example.weather.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weather.network.AuthStep
import com.example.weather.network.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(authViewModel: AuthViewModel = viewModel()) {
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val successMessage by authViewModel.successMessage.collectAsState()
    val authStep by authViewModel.authStep.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    var nameInput by remember { mutableStateOf("") }
    var dobInput by remember { mutableStateOf("") }

    val timeLeft = remember { mutableIntStateOf(125) }

    LaunchedEffect(authStep) {
        if (authStep == AuthStep.SIGNUP_OTP || authStep == AuthStep.FORGOT_OTP) {
            timeLeft.intValue = 125
            while (timeLeft.intValue > 0) {
                delay(1000)
                timeLeft.intValue--
            }
        } else {
            otpInput = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Dashboard, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(50.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("WMMP", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

            Spacer(modifier = Modifier.height(40.dp))

            when (authStep) {
                AuthStep.LOGIN -> {
                    val isEmailVerified by authViewModel.isEmailVerified.collectAsState()
                    val isVerifyingEmail by authViewModel.isVerifyingEmail.collectAsState()
                    val highlightForgot by authViewModel.highlightForgot.collectAsState()

                    AuthHeader("Welcome back", "Sign in to your account")

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            if (isEmailVerified) authViewModel.resetEmailVerification()
                        },
                        label = { Text("Email ID", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF151515), unfocusedContainerColor = Color(0xFF151515),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFF865D46), unfocusedIndicatorColor = Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        trailingIcon = {
                            if (emailInput.isNotBlank()) {
                                if (isVerifyingEmail) {
                                    CircularProgressIndicator(color = Color(0xFFA07BFF), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (isEmailVerified) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF198754), modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Outlined.CheckCircleOutline, contentDescription = "Verify", tint = Color.Gray, modifier = Modifier.size(24.dp).clickable {
                                        authViewModel.verifyEmail(emailInput.trim())
                                    })
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = isEmailVerified,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF151515), unfocusedContainerColor = Color(0xFF151515),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color(0xFF865D46), unfocusedIndicatorColor = Color(0xFF333333),
                                disabledContainerColor = Color(0xFF151515), disabledTextColor = Color.Gray,
                                disabledIndicatorColor = Color(0xFF333333)
                            ),
                            shape = RoundedCornerShape(12.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                        )

                        if (!isEmailVerified) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { authViewModel.setErrorMessage("Please tap the check icon in the email field first!") }
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = "Forgot your password?",
                            color = if (highlightForgot) Color(0xFFFF6B6B) else Color(0xFFA07BFF),
                            fontSize = 14.sp,
                            fontWeight = if (highlightForgot) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.clickable { authViewModel.setAuthStep(AuthStep.FORGOT_EMAIL) }
                        )
                    }

                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))

                    FullWidthButton("Sign In", isLoading, emailInput.isNotBlank() && passwordInput.isNotBlank() && isEmailVerified) {
                        authViewModel.signIn(emailInput.trim(), passwordInput)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                        Text("  OR  ", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF333333))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedFullWidthButton("Sign Up", isLoading = false) {
                        authViewModel.setAuthStep(AuthStep.SIGNUP_EMAIL)
                    }
                }

                AuthStep.SIGNUP_EMAIL -> {
                    AuthHeader("Create Account", "Enter your email to get started")
                    CustomTextField(value = emailInput, onValueChange = { emailInput = it }, label = "Enter Email ID", type = KeyboardType.Email)
                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))
                    FullWidthButton("Send OTP", isLoading, emailInput.isNotBlank()) { authViewModel.checkUserAndSendOtp(emailInput.trim()) }
                    BackToLogin { authViewModel.setAuthStep(AuthStep.LOGIN) }
                }
                AuthStep.SIGNUP_OTP -> {
                    AuthHeader("Verify Email", "We've sent a 6-digit code to your email")
                    OtpInputField(otpText = otpInput, onOtpChange = { otpInput = it })
                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))
                    CountdownTimer(timeLeft.intValue) { authViewModel.resendOtp(); timeLeft.intValue = 125 }
                    Spacer(modifier = Modifier.height(24.dp))
                    FullWidthButton("Verify", isLoading, otpInput.length == 6) { authViewModel.verifySignUpOtp(otpInput.trim()) }
                    BackToLogin { authViewModel.setAuthStep(AuthStep.LOGIN) }
                }
                AuthStep.SIGNUP_PASSWORD -> {
                    AuthHeader("Secure Account", "Create a strong password")
                    CustomTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = "Enter Password", type = KeyboardType.Password, isPassword = true)
                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))
                    FullWidthButton("Submit", isLoading, passwordInput.length >= 6) { authViewModel.submitNewUserPassword(passwordInput) }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cancel & Go Back",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { authViewModel.cancelSignup() }
                    )
                }

                AuthStep.SETUP_PROFILE -> {
                    AuthHeader("Setup Profile", "Tell us a bit about yourself")

                    CustomTextField(
                        value = nameInput,
                        onValueChange = { if(it.length <= 15) nameInput = it },
                        label = "Full Name (Max 15 letters)",
                        type = KeyboardType.Text
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 🟢 NAYA FIX: Flawless DOB formatting (without premature 0 padding)
                    OutlinedTextField(
                        value = dobInput,
                        onValueChange = { input ->
                            if (input.length <= 10) {
                                // 1. Remove anything that isn't a number
                                var clean = input.replace(Regex("[^0-9]"), "")
                                if (clean.length > 8) clean = clean.take(8)

                                // 2. Validate max days (31) and max months (12) silently
                                if (clean.length >= 2) {
                                    val d = clean.substring(0, 2).toIntOrNull() ?: 1
                                    if (d > 31) clean = "31" + clean.substring(2)
                                    if (d == 0) clean = "01" + clean.substring(2)
                                }
                                if (clean.length >= 4) {
                                    val m = clean.substring(2, 4).toIntOrNull() ?: 1
                                    if (m > 12) clean = clean.substring(0, 2) + "12" + clean.substring(4)
                                    if (m == 0) clean = clean.substring(0, 2) + "01" + clean.substring(4)
                                }

                                // 3. Rebuild string with slashes at the right positions
                                val res = StringBuilder()
                                for (i in clean.indices) {
                                    if (i == 2 || i == 4) res.append("/")
                                    res.append(clean[i])
                                }

                                // 4. Auto-append slash only if the user is typing forward
                                if (input.length > dobInput.length) {
                                    if (clean.length == 2 || clean.length == 4) {
                                        res.append("/")
                                    }
                                }

                                dobInput = res.toString()
                            }
                        },
                        label = { Text("Date of Birth (DD/MM/YYYY)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF151515), unfocusedContainerColor = Color(0xFF151515),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFF865D46), unfocusedIndicatorColor = Color(0xFF333333)
                        ),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )

                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))

                    FullWidthButton("Complete Setup", isLoading, nameInput.isNotBlank() && dobInput.length == 10) {
                        authViewModel.saveInitialProfile(nameInput, dobInput)
                    }
                }

                AuthStep.FORGOT_EMAIL -> {
                    AuthHeader("Reset Password", "Enter email to receive reset code")
                    CustomTextField(value = emailInput, onValueChange = { emailInput = it }, label = "Enter Email ID", type = KeyboardType.Email)
                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))
                    FullWidthButton("Send OTP", isLoading, emailInput.isNotBlank()) { authViewModel.sendForgotOtp(emailInput.trim()) }
                    BackToLogin { authViewModel.setAuthStep(AuthStep.LOGIN) }
                }
                AuthStep.FORGOT_OTP -> {
                    AuthHeader("Verify Reset Code", "Enter the OTP sent to your email")
                    OtpInputField(otpText = otpInput, onOtpChange = { otpInput = it })
                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))
                    CountdownTimer(timeLeft.intValue) { authViewModel.resendOtp(); timeLeft.intValue = 125 }
                    Spacer(modifier = Modifier.height(24.dp))
                    FullWidthButton("Verify", isLoading, otpInput.length == 6) { authViewModel.verifyForgotOtp(otpInput.trim()) }
                    BackToLogin { authViewModel.setAuthStep(AuthStep.LOGIN) }
                }
                AuthStep.RESET_PASSWORD -> {
                    AuthHeader("New Password", "Set your new password to login")
                    CustomTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = "New Password", type = KeyboardType.Password, isPassword = true)
                    ShowFeedback(errorMessage, successMessage)
                    Spacer(modifier = Modifier.height(24.dp))
                    FullWidthButton("Login", isLoading, passwordInput.length >= 6) { authViewModel.submitResetPassword(passwordInput) }
                }
            }
        }
    }
}

// --- REUSABLE UI COMPONENTS ---
@Composable
fun AuthHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, label: String, type: KeyboardType, isPassword: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF151515), unfocusedContainerColor = Color(0xFF151515),
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color(0xFF865D46), unfocusedIndicatorColor = Color(0xFF333333)
        ),
        shape = RoundedCornerShape(12.dp), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Next)
    )
}

@Composable
fun OtpInputField(otpText: String, onOtpChange: (String) -> Unit) {
    BasicTextField(
        value = otpText, onValueChange = { if (it.length <= 6) onOtpChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                repeat(6) { index ->
                    val char = if (index < otpText.length) otpText[index].toString() else ""
                    val isFocused = otpText.length == index
                    Box(
                        modifier = Modifier.size(45.dp)
                            .border(width = if (isFocused) 2.dp else 1.dp, color = if (isFocused) Color(0xFFA07BFF) else Color(0xFF333333), shape = RoundedCornerShape(8.dp))
                            .background(Color(0xFF151515)),
                        contentAlignment = Alignment.Center
                    ) { Text(text = char, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    )
}

@Composable
fun CountdownTimer(timeLeft: Int, onResend: () -> Unit) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        if (timeLeft > 0) Text("Resend OTP in ${String.format("%02d:%02d", minutes, seconds)}", color = Color.Gray, fontSize = 14.sp)
        else Text("Resend OTP", color = Color(0xFFA07BFF), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onResend() })
    }
}

@Composable
fun FullWidthButton(text: String, isLoading: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, disabledContainerColor = Color.DarkGray),
        shape = RoundedCornerShape(12.dp), enabled = isEnabled && !isLoading
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
        else Text(text, color = if(isEnabled) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun OutlinedFullWidthButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(55.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF444444)), shape = RoundedCornerShape(12.dp), enabled = !isLoading
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        else Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun ShowFeedback(errorMessage: String?, successMessage: String?) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!errorMessage.isNullOrEmpty()) { Spacer(modifier = Modifier.height(12.dp)); Text(text = errorMessage, color = Color(0xFFFF6B6B), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        if (!successMessage.isNullOrEmpty()) { Spacer(modifier = Modifier.height(12.dp)); Text(text = successMessage, color = Color(0xFF198754), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun BackToLogin(onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Text("← Back to Login", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.clickable { onClick() })
}