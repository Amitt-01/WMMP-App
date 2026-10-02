package com.example.weather.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("dob") val dob: String? = null,
    @SerialName("doj") val doj: String,
    @SerialName("last_name_change") val lastNameChange: String? = null
)