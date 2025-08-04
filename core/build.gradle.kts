plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.bithermmanagement.core"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")

    // Google Sheets API & Auth
    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
    implementation("com.google.apis:google-api-services-sheets:v4-rev516-1.23.0") {
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
    implementation("com.google.http-client:google-http-client-jackson2:1.23.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Google Play Services - Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Room
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    kapt("androidx.room:room-compiler:2.5.0")

    // Security
    implementation("androidx.security:security-crypto:1.0.0")
}
