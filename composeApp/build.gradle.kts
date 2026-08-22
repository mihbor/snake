import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
}

private val semanticVersionPattern = Regex("^[1-9][0-9]*\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)$")
private val releaseVersion = providers.gradleProperty("releaseVersion").orElse("1.0.0").get()
private val releaseVersionCode = providers.gradleProperty("releaseVersionCode").orElse("1").get().toIntOrNull()
    ?: error("releaseVersionCode must be a positive integer")
private val releaseTag = providers.gradleProperty("releaseTag").orNull

require(semanticVersionPattern.matches(releaseVersion)) {
    "releaseVersion must use semantic-version text with a positive major version such as 1.0.0"
}
require(releaseVersionCode > 0) { "releaseVersionCode must be a positive integer" }
if (releaseTag != null) {
    require(releaseTag == "v$releaseVersion") {
        "releaseTag must match releaseVersion as v<version>"
    }
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvmToolchain(21)

    androidTarget()
    jvm("desktop")
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        named("wasmJsMain") {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

android {
    namespace = "com.example.snake"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.snake"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = releaseVersionCode
        versionName = releaseVersion
    }
}

compose.desktop {
    application {
        mainClass = "com.example.snake.MainKt"

        buildTypes.release.proguard {
            version.set("7.5.0")
            configurationFiles.from(project.file("compose-desktop.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "snake"
            packageVersion = releaseVersion
        }
    }
}