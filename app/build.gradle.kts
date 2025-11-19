plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dondepp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dondepp"
        minSdk = 29
        targetSdk = 36
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

    // OSMDroid para mapas
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // Retrofit para requisições HTTP
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Gson para parsing JSON
    implementation("com.google.code.gson:gson:2.10.1")
    // Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
}