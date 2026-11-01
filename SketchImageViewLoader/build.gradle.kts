plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.mikaelzero.mojito.view.sketch"
    compileSdk = 34

    defaultConfig {
        minSdk = 16
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.sketch.gif)
    implementation(libs.androidx.exifinterface)
    implementation(project(":mojito"))
}
