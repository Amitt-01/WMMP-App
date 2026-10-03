package com.example.weather.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class AuthStep {
    LOGIN,
    SIGNUP_EMAIL, SIGNUP_OTP, SIGNUP_PASSWORD, SETUP_PROFILE,
    FORGOT_EMAIL, FORGOT_OTP, RESET_PASSWORD
}

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Auth)
    install(Postgrest)
}

data class UserData(val id: String, val email: String?)

@Serializable
data class InitialProfileUpsert(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("dob") val dob: String
)

@Serializable
data class NameUpdate(
    @SerialName("full_name") val fullName: String,
    @SerialName("last_name_change") val lastNameChange: String
)

@Serializable
data class DobUpdate(
    @SerialName("dob") val dob: String
)

@Serializable
data class AccountHash(
    @SerialName("email_hash") val emailHash: String
)

class AuthViewModel : ViewModel() {

    private val _user = MutableStateFlow<UserData?>(null)
    val user: StateFlow<UserData?> = _user

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _authStep = MutableStateFlow(AuthStep.LOGIN)
    val authStep: StateFlow<AuthStep> = _authStep

    var tempEmail = ""

    private val _settingsLoading = MutableStateFlow(false)
    val settingsLoading: StateFlow<Boolean> = _settingsLoading

    private val _settingsError = MutableStateFlow<String?>(null)
    val settingsError: StateFlow<String?> = _settingsError

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified

    private val _isVerifyingEmail = MutableStateFlow(false)
    val isVerifyingEmail: StateFlow<Boolean> = _isVerifyingEmail

    private val _highlightForgot = MutableStateFlow(false)
    val highlightForgot: StateFlow<Boolean> = _highlightForgot

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        if (_authStep.value == AuthStep.LOGIN && _user.value == null) {
                            val sessionUser = status.session.user
                            if (sessionUser != null) {
                                _user.value = UserData(id = sessionUser.id, email = sessionUser.email)
                                fetchUserProfile(sessionUser.id)
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _user.value = null
                        _userProfile.value = null
                    }
                    else -> {}
                }
            }
        }
    }

    private fun hashEmail(email: String): String {
        val bytes = email.trim().lowercase().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profile = supabase.postgrest["profiles"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<UserProfile>()

                if (profile == null) {
                    _authStep.value = AuthStep.SETUP_PROFILE
                    return@launch
                }
                if (profile.fullName.isNullOrBlank() || profile.dob.isNullOrBlank()) {
                    _userProfile.value = profile
                    _authStep.value = AuthStep.SETUP_PROFILE
                    return@launch
                }
                _userProfile.value = profile
                _authStep.value = AuthStep.LOGIN
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("401", ignoreCase = true) || errorMsg.contains("Unauthorized", ignoreCase = true)) {
                    signOut()
                } else {
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveInitialProfile(name: String, dob: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // 🟢 NAYA FIX: Delete account automatically if difference is > 80 years
            try {
                val dobParts = dob.split("/")
                if (dobParts.size == 3) {
                    val birthYear = dobParts[2].toInt()
                    val currentYear = java.time.Year.now().value
                    if ((currentYear - birthYear) > 80) {
                        deleteAccountForever {
                            _authStep.value = AuthStep.LOGIN
                            _errorMessage.value = "Age cannot be more than 80 years. Account deleted automatically."
                            signOut()
                        }
                        _isLoading.value = false
                        return@launch
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            val userId = _user.value?.id ?: return@launch
            try {
                supabase.postgrest["profiles"].upsert(InitialProfileUpsert(userId, name.trim(), dob.trim()))
                _successMessage.value = "Profile Setup Complete!"
                delay(1000)
                _authStep.value = AuthStep.LOGIN
                fetchUserProfile(userId)
            } catch (e: Exception) {
                e.printStackTrace()
                val exactError = e.message ?: "Unknown Error"
                if (exactError.contains("foreign key constraint", ignoreCase = true)) {
                    signOut()
                } else {
                    _errorMessage.value = "Error: $exactError"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateName(newName: String, onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = _user.value?.id ?: return@launch
            val currentProfile = _userProfile.value
            if (newName.trim().length > 15) {
                onResult("Name cannot exceed 15 characters.", false)
                return@launch
            }
            if (currentProfile?.lastNameChange != null) {
                try {
                    val lastChangeTime = Instant.parse(currentProfile.lastNameChange)
                    val now = Instant.now()
                    val daysPassed = ChronoUnit.DAYS.between(lastChangeTime, now)
                    if (daysPassed < 24) {
                        val daysLeft = 24 - daysPassed
                        onResult("You can change your name again after $daysLeft days.", false)
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            try {
                val nowStr = Instant.now().toString()
                supabase.postgrest["profiles"].update(NameUpdate(newName.trim(), nowStr)) {
                    filter { eq("id", userId) }
                }
                fetchUserProfile(userId)
                onResult("Name updated successfully!", true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("Error: ${e.message}", false)
            }
        }
    }


    fun checkDobMatch(inputDob: String): Boolean {
        return _userProfile.value?.dob == inputDob.trim()
    }

    private fun getLevenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    fun changePasswordWithOld(oldPass: String, newPass: String, onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            _settingsLoading.value = true
            try {
                val userEmail = _user.value?.email ?: throw Exception("User not found")

                if (getLevenshteinDistance(oldPass, newPass) < 2) {
                    onResult("New password must be significantly different from the old one.", false)
                    return@launch
                }

                try {
                    supabase.auth.signInWith(Email) {
                        this.email = userEmail
                        this.password = oldPass
                    }
                } catch (e: Exception) {
                    onResult("Incorrect old password.", false)
                    return@launch
                }

                supabase.auth.modifyUser { this.password = newPass }
                onResult("Password changed successfully", true)
            } catch (e: Exception) {
                onResult(e.message ?: "Failed to change password", false)
            } finally {
                _settingsLoading.value = false
            }
        }
    }

    fun requestSettingsOtp(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsError.value = null
            try {
                val userEmail = _user.value?.email ?: throw Exception("User Email not found")
                supabase.auth.resetPasswordForEmail(userEmail)
                onResult(true)
            } catch (e: Exception) {
                _settingsError.value = e.message ?: "Failed to send OTP"
                onResult(false)
            } finally {
                _settingsLoading.value = false
            }
        }
    }

    fun verifySettingsOtp(otp: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsError.value = null
            try {
                val userEmail = _user.value?.email ?: throw Exception("User Email not found")
                supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = userEmail, token = otp.trim())
                onResult(true)
            } catch (e: Exception) {
                _settingsError.value = "Invalid OTP. Try again."
                onResult(false)
            } finally {
                _settingsLoading.value = false
            }
        }
    }

    fun updatePasswordFromSettings(newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsError.value = null
            try {
                supabase.auth.modifyUser { this.password = newPassword }
                onResult(true)
            } catch (e: Exception) {
                _settingsError.value = e.message ?: "Failed to update password"
                onResult(false)
            } finally {
                _settingsLoading.value = false
            }
        }
    }

    fun deleteAccountForever(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _settingsLoading.value = true
            _settingsError.value = null
            try {
                val userEmail = _user.value?.email
                if (userEmail != null) {
                    val hashed = hashEmail(userEmail)
                    try {
                        supabase.postgrest["account_hashes"].delete { filter { eq("email_hash", hashed) } }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                supabase.postgrest.rpc("delete_user")
                onResult(true)
            } catch (e: Exception) {
                _settingsError.value = e.message ?: "Failed to delete account"
                onResult(false)
            } finally {
                _settingsLoading.value = false
            }
        }
    }

    fun verifyEmail(email: String) {
        viewModelScope.launch {
            _isVerifyingEmail.value = true
            _errorMessage.value = null
            _highlightForgot.value = false
            try {
                val hash = hashEmail(email)
                val result = supabase.postgrest["account_hashes"]
                    .select { filter { eq("email_hash", hash) } }
                    .decodeList<AccountHash>()

                if (result.isNotEmpty()) {
                    _isEmailVerified.value = true
                } else {
                    _errorMessage.value = "Account does not exist!"
                    _isEmailVerified.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error checking account."
                _isEmailVerified.value = false
            } finally {
                _isVerifyingEmail.value = false
            }
        }
    }

    fun resetEmailVerification() {
        _isEmailVerified.value = false
        _highlightForgot.value = false
    }

    fun setErrorMessage(msg: String) {
        _errorMessage.value = msg
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _highlightForgot.value = false
            try {
                supabase.auth.signInWith(Email) { this.email = email; this.password = pass }
                updateUserState()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("Invalid", true)) {
                    _errorMessage.value = "Incorrect Password!"
                    _highlightForgot.value = true // Highlight forgot password option
                } else {
                    _errorMessage.value = "Login Failed."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkUserAndSendOtp(email: String) {
        viewModelScope.launch {
            _isLoading.value = true; _errorMessage.value = null; _successMessage.value = null
            try {
                tempEmail = email
                supabase.auth.signUpWith(Email) { this.email = email; this.password = "Temp@12345!" }
                _successMessage.value = "OTP Sent Successfully!"
                delay(1500)
                _successMessage.value = null
                _authStep.value = AuthStep.SIGNUP_OTP
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifySignUpOtp(otp: String) {
        viewModelScope.launch {
            _isLoading.value = true; _errorMessage.value = null
            try {
                supabase.auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = tempEmail, token = otp)
                _authStep.value = AuthStep.SIGNUP_PASSWORD
            } catch (e: Exception) {
                _errorMessage.value = "Invalid OTP."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitNewUserPassword(password: String) {
        viewModelScope.launch {
            _isLoading.value = true; _errorMessage.value = null
            try {
                supabase.auth.modifyUser { this.password = password }
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    try {
                        val userEmail = session.user?.email
                        if (userEmail != null) {
                            val hashed = hashEmail(userEmail)
                            supabase.postgrest["account_hashes"].insert(AccountHash(hashed))
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    _user.value = UserData(id = session.user?.id ?: "", email = session.user?.email)
                    _authStep.value = AuthStep.SETUP_PROFILE
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendForgotOtp(email: String) {
        viewModelScope.launch {
            _isLoading.value = true; _errorMessage.value = null
            try {
                tempEmail = email
                supabase.auth.resetPasswordForEmail(email)
                _authStep.value = AuthStep.FORGOT_OTP
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyForgotOtp(otp: String) {
        viewModelScope.launch {
            _isLoading.value = true; _errorMessage.value = null
            try {
                supabase.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = tempEmail, token = otp)
                _authStep.value = AuthStep.RESET_PASSWORD
            } catch (e: Exception) {
                _errorMessage.value = "Invalid OTP"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitResetPassword(password: String) {
        viewModelScope.launch {
            _isLoading.value = true; _errorMessage.value = null
            try {
                supabase.auth.modifyUser { this.password = password }
                updateUserState()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resendOtp() {
        viewModelScope.launch {
            try {
                if (_authStep.value == AuthStep.SIGNUP_OTP) supabase.auth.resendEmail(OtpType.Email.SIGNUP, tempEmail)
                else if (_authStep.value == AuthStep.FORGOT_OTP) supabase.auth.resetPasswordForEmail(tempEmail)
            } catch (e: Exception) {}
        }
    }

    fun setAuthStep(step: AuthStep) {
        _errorMessage.value = null
        _successMessage.value = null
        _authStep.value = step
        _isEmailVerified.value = false
        _highlightForgot.value = false
    }

    private fun updateUserState() {
        val session = supabase.auth.currentSessionOrNull()
        if (session != null) {
            val sessionUser = session.user
            if (sessionUser != null) {
                _user.value = UserData(id = sessionUser.id, email = sessionUser.email)
                fetchUserProfile(sessionUser.id)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (e: Exception) { }
            finally { _user.value = null; _userProfile.value = null; setAuthStep(AuthStep.LOGIN) }
        }
    }

    fun cancelSignup() {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (e: Exception) { }
            finally { _user.value = null; setAuthStep(AuthStep.LOGIN) }
        }
    }
}