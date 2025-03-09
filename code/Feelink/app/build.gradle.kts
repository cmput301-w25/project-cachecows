plugins {
    id ("com.android.application")
    id ("com.google.gms.google-services") // Firebase plugin
}


android {
    namespace = "com.example.feelink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.feelink"
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


    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-storage")
    implementation ("com.google.android.gms:play-services-tasks:18.2.0")
    implementation(libs.espresso.intents)
    implementation(libs.firebase.storage)
//    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.5.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.0")
    testImplementation("com.google.firebase:firebase-auth:22.0.0")
    testImplementation("com.google.firebase:firebase-firestore:24.6.0")
    testImplementation("org.robolectric:robolectric:4.9")



    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}


