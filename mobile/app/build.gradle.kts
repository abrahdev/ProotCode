import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

val appVersion = rootProject.file("../VERSION").readText().trim()
val numericVersion = appVersion.substringBefore("-")
val versionParts = numericVersion.split(".")
val versionCodeValue =
    versionParts[0].toInt() * 10000 +
        versionParts.getOrElse(1) { "0" }.toInt() * 100 +
        versionParts.getOrElse(2) { "0" }.toInt()

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            FileInputStream(keystorePropertiesFile).use { load(it) }
        }
    }

android {
    namespace = "dev.prootcode.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.prootcode.app"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeValue
        versionName = appVersion
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (keystorePropertiesFile.exists()) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("boolean", "BUNDLED_ARTIFACTS", "false")
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        create("raccoonArm64") {
            dimension = "distribution"
            versionNameSuffix = "-Raccoon"
            buildConfigField("boolean", "BUNDLED_ARTIFACTS", "true")
            ndk {
                abiFilters += "arm64-v8a"
            }
        }
        create("raccoonX64") {
            dimension = "distribution"
            versionNameSuffix = "-Raccoon"
            buildConfigField("boolean", "BUNDLED_ARTIFACTS", "true")
            ndk {
                abiFilters += "x86_64"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("gz", "bin")
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint {
    android.set(true)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register<Exec>("bundleArtifacts") {
    group = "build"
    description = "Downloads and verifies the Raccoon artifacts into the flavor assets."
    workingDir = rootProject.projectDir
    commandLine("python3", "tools/bundle_artifacts.py")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.commons.compress)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.org.json)
    testRuntimeOnly(libs.junit.platform.launcher)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
