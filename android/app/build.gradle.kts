import java.util.Properties

val props = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.fenlight.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fenlight.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.4.0"
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"${props["TMDB_READ_ACCESS_TOKEN"]}\"")
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"${props["TRAKT_CLIENT_ID"]}\"")
        buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"${props["TRAKT_CLIENT_SECRET"]}\"")
        buildConfigField("String", "RD_CLIENT_ID", "\"${props["RD_CLIENT_ID"]}\"")
    }

    signingConfigs {
        // Only configured when local.properties provides a keystore (absent on CI).
        (props["KEYSTORE_FILE"] as String?)?.let { keystorePath ->
            create("release") {
                storeFile = file(keystorePath)
                storePassword = props["KEYSTORE_PASSWORD"] as String
                keyAlias = "fenlight"
                keyPassword = props["KEY_PASSWORD"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.moshi)
    testImplementation(libs.moshi.kotlin)
    // Android's org.json is a stub in local unit tests; KodiDiscovery parses with JSONObject
    testImplementation(libs.org.json)
}
