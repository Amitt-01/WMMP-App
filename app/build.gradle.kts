import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.0"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.weather"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.weather"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "WEATHER_BASE_URL", "\"${localProperties.getProperty("WEATHER_BASE_URL")}\"")
        buildConfigField("String", "AUDIUS_BASE_URL", "\"${localProperties.getProperty("AUDIUS_BASE_URL")}\"")
        buildConfigField("String", "OSRM_BASE_URL", "\"${localProperties.getProperty("OSRM_BASE_URL")}\"")
        buildConfigField("String", "MAP_TILE_URL", "\"${localProperties.getProperty("MAP_TILE_URL")}\"")



        // 1. NATIVE LIBRARIES OPTIMIZATION
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    buildTypes {
        release {
            // 2. PROGUARD and RESOURCE SHRINKING
            isMinifyEnabled = true // Unused code and extra libraries auto delete
            isShrinkResources = true // Unused img, icons and XML file
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation & UI
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")

    // Retrofit & Lifecycle
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Location & Maps
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.maplibre.gl:android-sdk:11.4.0")

    // Media & Images
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Supabase (Auth + Database + Ktor)
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.3")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.3")
    implementation("io.ktor:ktor-client-android:2.3.9")

    // Kotlin Serialization JSON dependency
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Modern Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.2.1")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.1")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
}