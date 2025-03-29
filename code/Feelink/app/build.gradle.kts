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

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
//    implementation(files("C:/Users/jcyri/AppData/Local/Android/Sdk/platforms/android-35/android.jar")) //specifc to Cyrils local machine . This is for javadocs
    implementation(libs.firebase.auth)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.espresso.intents)
    implementation(libs.firebase.storage)
    implementation(libs.recyclerview)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.work.runtime)
    implementation ("com.google.firebase:firebase-messaging:23.1.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("androidx.test.espresso:espresso-idling-resource:3.5.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testImplementation("org.mockito:mockito-android:4.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation ("com.google.android.gms:play-services-tasks:18.2.0")
    androidTestImplementation ("androidx.test.espresso:espresso-intents:3.5.1")
//    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.google.code.gson:gson:2.12.1")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("com.airbnb.android:lottie:6.6.3")
    implementation ("com.google.android.libraries.places:places:3.3.0")
    implementation ("com.google.android.gms:play-services-maps:18.0.2")
    implementation ("com.google.android.libraries.places:places:2.5.0")
    // Google Maps dependencies
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    implementation ("com.google.code.gson:gson:2.12.1")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("com.airbnb.android:lottie:6.6.3")
}