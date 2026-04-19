import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.samvaad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.samvaad"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose Groq API key to the app safely via BuildConfig
        var groqKey = ""
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            val localProperties = Properties()
            localProperties.load(FileInputStream(localPropertiesFile))
            groqKey = localProperties.getProperty("GROQ_API_KEY") ?: ""
        }
        buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)

    // Retrofit (Exp 6)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Room Database (Exp 10)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // CameraX (Exp 7)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.6")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Charting & Data Visualizations (Analytics Engine)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")

    // Background Tasks
    implementation(libs.work.runtime)

    // Logging
    implementation(libs.okhttp.logging)

    // Guava (Async callbacks)
    implementation("com.google.guava:guava:33.2.1-android")
}