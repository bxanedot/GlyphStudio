plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    id("maven-publish")
}

kotlin {
    android {
        namespace = "com.composables.sfsymbols"
        compileSdk = 37
        minSdk = 21
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    js {
        browser()
        nodejs()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.ui)
            api(libs.compose.ui.graphics)
        }

        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            // Skia natives for the headless render tests (picked for the machine running them)
            val os = System.getProperty("os.name").lowercase()
            val arm = System.getProperty("os.arch").lowercase().let { it.contains("aarch64") || it.contains("arm64") }
            val skikoOs = when {
                os.contains("win") -> "windows-x64"
                os.contains("mac") -> if (arm) "macos-arm64" else "macos-x64"
                else -> if (arm) "linux-arm64" else "linux-x64"
            }
            implementation("org.jetbrains.skiko:skiko-awt-runtime-$skikoOs:0.150.1")
        }
    }
}
