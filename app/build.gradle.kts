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
        buildConfigField("String", "LEVELPLAY_APP_KEY", "\"\"")
        buildConfigField("String", "LEVELPLAY_REWARDED_AD_UNIT", "\"\"")
        buildConfigField("String", "LEVELPLAY_INTERSTITIAL_AD_UNIT", "\"\"")
        buildConfigField("String", "LEVELPLAY_BANNER_AD_UNIT", "\"\"")
    }
    buildTypes { release { isMinifyEnabled = false } }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.unity3d.ads-mediation:mediation-sdk:9.5.0")
}
