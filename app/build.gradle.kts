plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.weearntogether.play"
    val ciKey = System.getenv("WET_SIGNING_KEYSTORE")
    if (!ciKey.isNullOrBlank()) {
        signingConfigs {
            getByName("debug") {
                storeFile = file(ciKey)
                storePassword = System.getenv("WET_SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("WET_SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("WET_SIGNING_KEY_PASSWORD")
            }
        }
    }
    compileSdk = 35
    defaultConfig {
        applicationId = "com.weearntogether.play"
        minSdk = 24
        targetSdk = 35
        // Every CI artifact must have a newer Android versionCode so testers can install
        // the next APK as an update instead of Android rejecting an equal/older package.
        val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = ciRunNumber ?: 1
        versionName = if (ciRunNumber != null) "0.1.$ciRunNumber" else "0.1.0"
        buildConfigField("String", "LEVELPLAY_APP_KEY", "\"28e1c919d\"")
        buildConfigField("String", "LEVELPLAY_REWARDED_AD_UNIT", "\"ui8a3ve6ggugacuo\"")
        buildConfigField("String", "LEVELPLAY_INTERSTITIAL_AD_UNIT", "\"onmeefn82676cjk8\"")
        buildConfigField("String", "LEVELPLAY_BANNER_AD_UNIT", "\"3z9a2p5o88cx61ls\"")
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
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    testImplementation("junit:junit:4.13.2")
    implementation("com.unity3d.ads-mediation:mediation-sdk:9.5.0")
}
