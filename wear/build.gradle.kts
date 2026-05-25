plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.alphahealth.monitor.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alphahealth.monitor.wear"
        // Wear OS 3 (API 30) — covers Galaxy Watch 4/5/6/7, Pixel Watch 1/2/3,
        // Mobvoi TicWatch Pro 5, Fossil Gen 6, and all other Wear OS 3+ devices.
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystoreFile = file("release.jks")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Shared module ──────────────────────────────────────────────────────
    implementation(project(":shared"))

    // ── Samsung Health Sensor SDK (OPTIONAL — Galaxy Watch only) ──────────
    // Provides raw EDA, MF-BIA, PPG sensor access on Galaxy Watch 4+.
    // On non-Samsung Wear OS devices the app uses Android Health Services
    // as the fallback sensor layer (heart rate, SpO2, steps, calories).
    // Place samsung-health-sensor-api.aar in wear/libs/ to enable.
    // Download: https://developer.samsung.com/health/sensor/guide/introduction.html
    val samsungSensorAar = file("libs/samsung-health-sensor-api.aar")
    if (samsungSensorAar.exists()) {
        implementation(files("libs/samsung-health-sensor-api.aar"))
    }

    // ── Android Health Services (universal Wear OS sensor API) ────────────
    // Provides PassiveMonitoringClient and ExerciseClient on ALL Wear OS
    // watches regardless of manufacturer. Used as primary on non-Samsung,
    // and as secondary fallback layer on Samsung when Galaxy sensors fail.
    implementation("androidx.health:health-services-client:1.1.0-alpha03")

    // ── Wear OS Compose ───────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.compose.material:material-icons-extended")

    // ── Wear OS Tiles + Complications ────────────────────────────────────
    implementation("androidx.wear.tiles:tiles:1.2.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // ── Wearable Data Layer (Bluetooth sync to phone) ─────────────────────
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("com.google.guava:guava:31.1-android")

    // ── Coroutines ────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ── Legacy support ────────────────────────────────────────────────────
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ── Testing ───────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
