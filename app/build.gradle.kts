plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.sleepfuriously.paulsapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sleepfuriously.paulsapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.30"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // for encrypted sharedprefs
    implementation(libs.androidx.security.crypto)

    // for startup splash screen animation
    implementation(libs.androidx.core.splashscreen)

    // to use composeWithLifeCycle() functions
    implementation(libs.androidx.lifecycle.runtime.compose.android)

    // using basic networking lib
    implementation(libs.okhttp)

    // for coroutines outside of Activities
    implementation(libs.androidx.lifecycle.runtime.ktx.v241)

    // we're using the gson lib
    implementation(libs.gson)

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // okhttp-sse
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    // json serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // large selection of built-in icons
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}