plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.projectcachecows"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.projectcachecows"
        minSdk = 24
        targetSdk = 35
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
<<<<<<< Updated upstream:code/app/build.gradle.kts
=======
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
>>>>>>> Stashed changes:code/Feelink/app/build.gradle.kts
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Add new library
    implementation("com.github.bumptech.glide:glide:4.16.0")
}