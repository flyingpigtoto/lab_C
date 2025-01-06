plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // Apply kotlin-kapt plugin
}

android {
    namespace = "com.example.lab_c"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lab_c"
        minSdk = 26
        targetSdk = 35 // Aligned with compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.9" // Explicitly set language version to 1.9
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // Core and AppCompat
    implementation(libs.androidx.core.ktx) // Ensure this points to androidx.core:core-ktx:1.10.1 in your version catalog
    implementation(libs.androidx.appcompat) // Ensure this points to androidx.appcompat:appcompat:1.6.1 in your version catalog

    // Other AndroidX dependencies via version catalogs
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Unit Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // Material Components
    implementation("com.google.android.material:material:1.9.0")

    // For charts (optional)
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")

    // CoordinatorLayout for Bottom Sheet
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Backward compatibility
    implementation("androidx.multidex:multidex:2.0.1")

    // Coroutine dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // RecyclerView for lists
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
}
