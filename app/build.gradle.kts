import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun getSigningProperty(propKey: String, envKey: String): String? {
    return System.getenv(envKey)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: (findProperty(propKey) as? String)?.takeIf { it.isNotBlank() }
}

val releaseStoreFilePath = getSigningProperty("storeFile", "KEYSTORE_PATH")
val releaseStorePassword = getSigningProperty("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = getSigningProperty("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = getSigningProperty("keyPassword", "KEY_PASSWORD")

val requireReleaseSigning = System.getenv("REQUIRE_RELEASE_SIGNING")?.equals("true", ignoreCase = true) == true ||
    (project.findProperty("requireReleaseSigning")?.toString()?.equals("true", ignoreCase = true) == true)

val hasAnySigningProperty = releaseStoreFilePath != null ||
    releaseStorePassword != null ||
    releaseKeyAlias != null ||
    releaseKeyPassword != null

val hasCompleteSigningCredentials = releaseStoreFilePath != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

if (requireReleaseSigning && !hasCompleteSigningCredentials) {
    if (!hasAnySigningProperty) {
        throw GradleException(
            "Release signing is required (strict mode enabled via REQUIRE_RELEASE_SIGNING=true or -PrequireReleaseSigning=true), " +
            "but no release signing credentials were provided."
        )
    } else {
        throw GradleException(
            "Release signing is required, but release signing credentials are incomplete. " +
            "All four properties (storeFile/KEYSTORE_PATH, storePassword/KEYSTORE_PASSWORD, keyAlias/KEY_ALIAS, keyPassword/KEY_PASSWORD) must be provided."
        )
    }
} else if (!requireReleaseSigning && hasAnySigningProperty && !hasCompleteSigningCredentials) {
    throw GradleException(
        "Incomplete release signing configuration provided. " +
        "All four properties (storeFile/KEYSTORE_PATH, storePassword/KEYSTORE_PASSWORD, keyAlias/KEY_ALIAS, keyPassword/KEY_PASSWORD) must be provided."
    )
}

val releaseStoreFile = releaseStoreFilePath?.let { rawPath ->
    val rawFile = File(rawPath)
    if (rawFile.isAbsolute) {
        rawFile
    } else {
        val fromRoot = rootProject.file(rawPath)
        if (fromRoot.exists()) fromRoot else file(rawPath)
    }
}

if (hasCompleteSigningCredentials) {
    if (releaseStoreFile == null || !releaseStoreFile.exists()) {
        throw GradleException(
            "Configured release keystore file was not found. " +
            "Specified path: '$releaseStoreFilePath'. " +
            "Please verify the keystore path is correct (relative to repository root or absolute)."
        )
    }
}

android {
    namespace = "com.example.notificationvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notificationvault"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCompleteSigningCredentials && releaseStoreFile != null && releaseStoreFile.exists()) {
            create("release") {
                storeFile = releaseStoreFile
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
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
    }

    sourceSets {
        getByName("test").assets.srcDirs("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
