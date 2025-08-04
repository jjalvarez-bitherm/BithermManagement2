plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // id("kotlin-kapt") // Comentado para evitar errores
    // id("com.google.dagger.hilt.android") // Comentado para evitar errores
}

android {
    namespace = "com.bithermmanagement.login"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":navigation"))

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    // kapt("com.google.dagger:hilt-android-compiler:2.48") // Comentado para evitar errores

    // Google Sheets API
    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
    implementation("com.google.apis:google-api-services-sheets:v4-rev516-1.23.0") {
        exclude(group = "com.google.guava", module = "guava-jdk5")
    }
    implementation("com.google.http-client:google-http-client-jackson2:1.23.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Lifecycle KTX for lifecycleScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
} 