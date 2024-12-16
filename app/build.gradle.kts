

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.raytechinnovators.bijlionn"
    compileSdk = 34

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }

    defaultConfig {
        applicationId = "com.raytechinnovators.bijlionn"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildFeatures {
        viewBinding = true
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
}

dependencies {
// Firebase
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
    implementation("com.google.firebase:firebase-crashlytics:19.0.0")
    implementation("com.google.firebase:firebase-firestore:25.0.0")
    implementation("com.google.firebase:firebase-messaging:24.0.0")

// Google Play Services
    implementation("com.google.android.gms:play-services-ads:23.4.0")
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

// UI Libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.mukeshsolanki:android-otpview-pinview:2.1.2")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("com.airbnb.android:lottie:6.1.0")

// Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.google.firebase:firebase-config-ktx:22.0.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

// Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

// Datastore
    implementation("androidx.datastore:datastore-core-android:1.1.1")

// Activity
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.activity:activity:1.8.0")

// Room
    implementation("androidx.room:room-ktx:2.6.1")

// Emoji
    implementation("androidx.emoji2:emoji2-emojipicker:1.5.0")

// Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

// Extra
    implementation("com.github.marlonlom:timeago:4.0.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation("com.github.GrenderG:Toasty:1.5.2")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.2")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")

    implementation ("com.google.android.play:integrity:1.4.0")
}
