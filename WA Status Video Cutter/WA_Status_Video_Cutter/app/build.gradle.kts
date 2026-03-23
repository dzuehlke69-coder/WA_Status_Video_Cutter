plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.wa_status_video_cutter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.wa_status_video_cutter"
        minSdk = 26
        targetSdk = 36
        versionCode = 126
        versionName = "2.0.0"
        ndk {
            abiFilters.add "arm64-v8a"
        }
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
    }
} // <--- Diese schließt 'android'

dependencies {
    // Falls JitPack aktiv ist, nutzt du diesen Pfad:
    implementation("com.arthenica:ffmpeg-kit-min:4.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
} // <--- Diese schließt 'dependencies'