plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mad"
        minSdk = 24
        targetSdk = 34
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.2")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.2")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    // Volley - Added for Database Connection
    implementation("com.android.volley:volley:1.2.1")
}
