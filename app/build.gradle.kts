plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    kotlin("plugin.serialization") version "1.9.20"
}

 import java.util.Properties

android {
    namespace = "com.swingtrace.aicoaching"
    compileSdk = 34

     val localProperties = Properties().apply {
         val localPropertiesFile = rootProject.file("local.properties")
         if (localPropertiesFile.exists()) {
             localPropertiesFile.inputStream().use { load(it) }
         }
     }

    defaultConfig {
        applicationId = "com.swingtrace.aicoaching.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val geminiApiKey = (localProperties.getProperty("GEMINI_API_KEY") ?: "")
            .replace("\"", "")
            .trim()
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Build Variants: a_practice を最優先で選択
        missingDimensionStrategy("version", "a_practice", "b_athlete", "c_pro")

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

    flavorDimensions += "version"

    productFlavors {
        create("a_practice") {
            dimension = "version"
            buildConfigField("boolean", "IS_PRACTICE_MODE", "true")
            resValue("string", "mode_label", "PRACTICE")
        }
        create("b_athlete") {
            dimension = "version"
            buildConfigField("boolean", "IS_PRACTICE_MODE", "false")
            resValue("string", "mode_label", "ATHLETE")
        }
        create("c_pro") {
            dimension = "version"
            buildConfigField("boolean", "IS_PRACTICE_MODE", "false")
            resValue("string", "mode_label", "PRO")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Video Player
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    
    // ML Kit for Object Detection
    implementation("com.google.mlkit:object-detection:17.0.1")
    
    // Coroutines for ML Kit
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // ONNX Runtime for YOLOv8 (IR version 10対応)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.0")
    
    // TensorFlow Lite (オプション)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // MediaPipe for Pose Detection
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // DataStore (FreeQuotaManager用)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    
    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    
    // Gemini AI (最新版)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    
    // Ktor dependencies (Gemini SDK依存)
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Kotlinx Serialization (Gemini依存)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
