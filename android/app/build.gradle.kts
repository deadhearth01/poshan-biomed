import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    // Untracked, machine-local. Holds the SDK path and the Appwrite backend config.
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }

    // Optional untracked override, for when you want backend config kept apart from the
    // SDK path — same keys, higher precedence.
    val secrets = rootProject.file("secrets.properties")
    if (secrets.exists()) secrets.inputStream().use { load(it) }
}

/**
 * Resolves a build-time setting, most specific source first.
 *
 *   -P on the command line  ->  environment variable  ->  local/secrets.properties  ->  default
 *
 * The environment step is what lets CI configure a build without writing a file: Gradle
 * property `poshan.appwrite.projectId` maps to `POSHAN_APPWRITE_PROJECTID`. Nothing in this
 * chain reads a committed file, so the repository carries no backend of its own.
 */
fun prop(key: String, fallback: String): String {
    val env = System.getenv(key.replace('.', '_').uppercase())
    return (findProperty(key) as String?)
        ?: env?.takeIf { it.isNotBlank() }
        ?: localProps.getProperty(key)
        ?: fallback
}

android {
    namespace = "com.teamposhan.biomed"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.teamposhan.biomed"
        minSdk = 24
        targetSdk = 36
        versionCode = 50
        versionName = "0.32.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "APPWRITE_ENDPOINT", "\"${prop("poshan.appwrite.endpoint", "https://cloud.appwrite.io/v1")}\"")
        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"${prop("poshan.appwrite.projectId", "poshan-dev")}\"")
        buildConfigField("String", "APPWRITE_DATABASE_ID", "\"${prop("poshan.appwrite.databaseId", "poshan_db")}\"")
        buildConfigField("boolean", "APPWRITE_ENABLED", prop("poshan.appwrite.enabled", "false"))

        // The OAuth callback scheme has to appear in the manifest, and it embeds the
        // project id. Injected from the same resolved property rather than hardcoded, so
        // the committed manifest carries no specific backend.
        manifestPlaceholders["appwriteProjectId"] =
            prop("poshan.appwrite.projectId", "poshan-dev")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.appwrite.sdk)
    implementation(libs.coil.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.face)
    implementation(libs.mediapipe.tasks)
    implementation(libs.androidx.work.runtime)
}
