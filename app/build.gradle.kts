import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.calmpulse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.calmpulse"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "1.7.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Injeção segura da chave de API via BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    val keystoreFilePath = System.getenv("KEYSTORE_FILE") 
        ?: localProperties.getProperty("KEYSTORE_FILE") 
        ?: "calmpulse-production.jks"
    val releaseStoreFile = file(keystoreFilePath)
    val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD") 
        ?: localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
    val releaseKeyAlias = System.getenv("KEY_ALIAS") 
        ?: localProperties.getProperty("KEY_ALIAS") ?: ""
    val releaseKeyPassword = System.getenv("KEY_PASSWORD") 
        ?: localProperties.getProperty("KEY_PASSWORD") ?: ""

    signingConfigs {
        create("release") {
            if (releaseStoreFile.exists() && releaseStorePassword.isNotBlank()) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null && releaseConfig.storeFile!!.exists()) {
                signingConfig = releaseConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.generativeai)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
}
