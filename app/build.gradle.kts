plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.todoshift.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.todoshift.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 28
        versionName = "1.1.0-rc7e"
    }


    signingConfigs {
        create("persistentDebug") {
            val keyPath = System.getenv("TODOSHIFT_SIGNING_FILE")
            val signingPassword = System.getenv("TODOSHIFT_SIGNING_PASSWORD")

            if (!keyPath.isNullOrBlank() &&
                !signingPassword.isNullOrBlank()) {
                storeFile = file(keyPath)
                storePassword = signingPassword
                keyAlias = "todoshift"
                keyPassword = signingPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("persistentDebug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
