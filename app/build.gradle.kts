plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    buildFeatures { viewBinding = true }
    namespace = "com.easy.peasy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ezirezo.repotin"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        setProperty("archivesBaseName", "com.ezirezo.repotin_v$versionCode")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

}

dependencies {

    implementation("com.onesignal:OneSignal:5.1.35")
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("com.appsflyer:af-android-sdk:6.17.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1") // або твоя версія

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}