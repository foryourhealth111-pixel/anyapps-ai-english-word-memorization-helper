import java.io.FileInputStream
import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

fun String.toBuildConfigStringLiteral(): String {
  return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

val apiBaseUrlFromProperty = (project.findProperty("WORDCOACH_API_BASE_URL") as? String)?.trim()
val clientTokenFromProperty = (project.findProperty("WORDCOACH_CLIENT_TOKEN") as? String)?.trim()

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
  FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

fun readSigningValue(name: String): String? {
  return (
    keystoreProperties.getProperty(name)
      ?: (project.findProperty(name) as? String)
      ?: System.getenv(name)
    )?.trim()?.takeIf { it.isNotEmpty() }
}

val releaseStoreFilePath = readSigningValue("WORDCOACH_RELEASE_STORE_FILE")
val releaseStorePassword = readSigningValue("WORDCOACH_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = readSigningValue("WORDCOACH_RELEASE_KEY_ALIAS")
val releaseKeyPassword = readSigningValue("WORDCOACH_RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig = !releaseStoreFilePath.isNullOrBlank() &&
  !releaseStorePassword.isNullOrBlank() &&
  !releaseKeyAlias.isNullOrBlank() &&
  !releaseKeyPassword.isNullOrBlank()

android {
  namespace = "com.wordcoach"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.wordcoach"
    minSdk = 29
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val apiBaseUrl = if (apiBaseUrlFromProperty.isNullOrEmpty()) {
      "http://10.0.2.2:8080"
    } else {
      apiBaseUrlFromProperty
    }
    val clientToken = if (clientTokenFromProperty.isNullOrEmpty()) {
      "replace-me-client-token"
    } else {
      clientTokenFromProperty
    }
    buildConfigField("String", "API_BASE_URL", apiBaseUrl.toBuildConfigStringLiteral())
    buildConfigField("String", "CLIENT_TOKEN", clientToken.toBuildConfigStringLiteral())
  }

  signingConfigs {
    if (hasReleaseSigningConfig) {
      create("release") {
        storeFile = rootProject.file(releaseStoreFilePath!!)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
        enableV1Signing = true
        enableV2Signing = true
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      if (hasReleaseSigningConfig) {
        signingConfig = signingConfigs.getByName("release")
      }
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
    debug {
      isDebuggable = true
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
  val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("com.google.android.material:material:1.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("com.facebook.shimmer:shimmer:0.5.0")
  implementation("io.noties.markwon:core:4.6.2")

  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

  implementation("com.google.mlkit:text-recognition:16.0.1")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
  testImplementation("io.mockk:mockk:1.13.12")
  testImplementation("androidx.arch.core:core-testing:2.2.0")

  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
