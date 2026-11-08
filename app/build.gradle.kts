import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val ssmBackend = providers.gradleProperty("ssmBackend").orElse("production").get().lowercase()
require(ssmBackend == "production" || ssmBackend == "staging") {
    "ssmBackend must be either 'production' or 'staging'."
}
val stagingBuild = ssmBackend == "staging"
val expectedSupabaseUrl = if (stagingBuild) {
    "https://jiqbxcqlywvvmhbgirzl.supabase.co"
} else {
    "https://rzskhndweihuczbdhoxk.supabase.co"
}
val configuredSupabaseUrl = localProperties.getProperty(if (stagingBuild) "SUPABASE_STAGING_URL" else "SUPABASE_URL")
    ?.takeIf { it.isNotBlank() }
val supabaseUrl = configuredSupabaseUrl ?: expectedSupabaseUrl
require(supabaseUrl.trimEnd('/') == expectedSupabaseUrl) {
    "Android SUPABASE_URL does not match the explicitly selected $ssmBackend backend."
}
val supabaseAnonKey = localProperties.getProperty(
    if (stagingBuild) "SUPABASE_STAGING_PUBLISHABLE_KEY" else "SUPABASE_ANON_KEY",
)
    ?.takeIf { it.isNotBlank() }
    ?: ""
require(supabaseAnonKey.isNotBlank()) {
    "The selected backend's publishable/anon key must be provided in ignored local.properties or the secure build environment."
}
require(!stagingBuild || gradle.startParameter.taskNames.none { it.contains("release", ignoreCase = true) }) {
    "Staging backend builds are restricted to local debug tasks; release builds must use production."
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        if (stagingBuild) {
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SCMS_BACKEND", "\"$ssmBackend\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
