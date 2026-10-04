import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseSigningProperties = Properties()
val releaseSigningPropertiesFile =
    rootProject.file("keystore.properties")

if (releaseSigningPropertiesFile.isFile) {
    releaseSigningPropertiesFile.inputStream().use {
        releaseSigningProperties.load(it)
    }
}

fun releaseSigningValue(name: String): String? =
    System.getenv(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: releaseSigningProperties
            .getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

val releaseStoreFilePath =
    releaseSigningValue("FLOENTLY_UPLOAD_STORE_FILE")
val releaseStorePassword =
    releaseSigningValue("FLOENTLY_UPLOAD_STORE_PASSWORD")
val releaseKeyAlias =
    releaseSigningValue("FLOENTLY_UPLOAD_KEY_ALIAS")
val releaseKeyPassword =
    releaseSigningValue("FLOENTLY_UPLOAD_KEY_PASSWORD")

val releaseSigningConfigured = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

val releaseTaskRequested =
    gradle.startParameter.taskNames.any {
        it.contains("release", ignoreCase = true)
            || it.contains("bundle", ignoreCase = true)
            || it.contains("publish", ignoreCase = true)
    }

if (releaseTaskRequested && !releaseSigningConfigured) {
    throw GradleException(
        "Floently Read release signing is not configured. " +
            "Set FLOENTLY_UPLOAD_STORE_FILE, " +
            "FLOENTLY_UPLOAD_STORE_PASSWORD, " +
            "FLOENTLY_UPLOAD_KEY_ALIAS and " +
            "FLOENTLY_UPLOAD_KEY_PASSWORD in keystore.properties " +
            "or CI secrets."
    )
}

android {
    namespace = "com.floently.read"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vitusidi.floently.read"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = rootProject.file(
                    requireNotNull(releaseStoreFilePath)
                )
                storePassword =
                    requireNotNull(releaseStorePassword)
                keyAlias =
                    requireNotNull(releaseKeyAlias)
                keyPassword =
                    requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".native.dev"
            versionNameSuffix = "-dev"
        }

        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig =
                    signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}


val readCoreNativeAbis = listOf(
    "armeabi-v7a",
    "arm64-v8a",
    "x86",
    "x86_64"
)

val verifyReadCoreNative by tasks.registering {
    doLast {
        val missing = readCoreNativeAbis.filter { abi ->
            !file(
                "src/main/jniLibs/$abi/libfloently_read_core_native.so"
            ).isFile
        }

        check(missing.isEmpty()) {
            "Missing Rust Read Core JNI libraries for: " +
                missing.joinToString() +
                ". Run scripts/build_read_native_core_android.sh " +
                "from the repository root before building Android."
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(verifyReadCoreNative)
}
