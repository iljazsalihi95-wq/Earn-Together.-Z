plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.weearntogether.play"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.weearntogether.play"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "UNITY_ANDROID_GAME_ID", "\"6209382\"")
        buildConfigField("String", "UNITY_REWARDED_AD_UNIT", "\"Rewarded_Android\"")
        buildConfigField("String", "UNITY_INTERSTITIAL_AD_UNIT", "\"Interstitial_Android\"")
        buildConfigField("String", "UNITY_BANNER_AD_UNIT", "\"Banner_Android\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
