plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.Mari.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.Mari.mobile"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Base URLs are configurable via Gradle properties for flexibility.
        // Defaults target Android emulator (host loopback is 10.0.2.2).
        val coreBaseUrl = (project.findProperty("coreBaseUrl") as String?) ?: "http://10.0.2.2:3000"
        val bankBaseUrl = (project.findProperty("bankBaseUrl") as String?) ?: "http://10.0.2.2:3001"
        buildConfigField("String", "CORE_BASE_URL", "\"$coreBaseUrl\"")
        // Optional: Pre-provisioned bank merchant UUID for demo settlement from mobile
        buildConfigField("String", "BANK_MERCHANT_ID", "\"\"")
        // Default location grid for demo when GPS/grid provider is not integrated
        buildConfigField("String", "DEFAULT_GRID", "\"grid123\"")
        // Bank base URL (mock bank default port 3001)
        buildConfigField("String", "BANK_BASE_URL", "\"$bankBaseUrl\"")
        // Comma-separated list of allowed SMS sender numbers (e.g., Twilio numbers) for receipt processing
        val allowedSmsSenders = (project.findProperty("allowedSmsSenders") as String?) ?: ""
        buildConfigField("String", "ALLOWED_SMS_SENDERS", "\"$allowedSmsSenders\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            // Debug uses defaults from defaultConfig (emulator by default).
        }
        // Build type optimized for physical devices on same LAN as the host.
        create("debugPhysical") {
            initWith(getByName("debug"))
            val physCore = (project.findProperty("coreBaseUrlPhysical") as String?)?.takeIf { it.isNotBlank() }
            val physBank = (project.findProperty("bankBaseUrlPhysical") as String?)?.takeIf { it.isNotBlank() }
            if (physCore != null) {
                buildConfigField("String", "CORE_BASE_URL", "\"$physCore\"")
            }
            if (physBank != null) {
                buildConfigField("String", "BANK_BASE_URL", "\"$physBank\"")
            }
            val physAllowed = (project.findProperty("allowedSmsSendersPhysical") as String?)?.takeIf { it.isNotBlank() }
            if (physAllowed != null) {
                buildConfigField("String", "ALLOWED_SMS_SENDERS", "\"$physAllowed\"")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Point the app module at the existing source folders in the repository root
    sourceSets {
        getByName("main") {
            java.setSrcDirs(listOf(
                "src/main/java", // IMPORTANT: include app module sources (Application, Activities)
                "../core",
                "../data",
                "../domain",
                "../di",
                "../presentation/ui",
                "../protocol",
                "../service",
                "../work",
                "../manager" // optional legacy; can be removed once migration is complete
            ))
            manifest.srcFile("src/main/AndroidManifest.xml")
            res.setSrcDirs(listOf("src/main/res"))
        }
    }
}

dependencies {
    // Align Kotlin libs across transitive dependencies
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.10"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.10")
    // Kotlin / Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.compose.material:material-icons-extended")

    // Material Components (View system) for Theme.Material3 parent (resource definitions)
    implementation("com.google.android.material:material:1.12.0")

    // Hilt (align with Kotlin 1.9.x)
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // WorkManager + Hilt worker
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Room (align with Kotlin 1.9.x / AGP 8)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Removed zstd-jni; using GZIP for SMS compression to avoid JNI on Android

    // Networking: Retrofit + OkHttp + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // CameraX for face capture
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // QR Code generation and scanning
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Location services (already in dependencies)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Testing (optional minimal)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
