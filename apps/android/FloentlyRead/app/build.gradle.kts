plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
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

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".native.dev"
            versionNameSuffix = "-dev"
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
