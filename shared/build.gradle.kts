import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // Agregar el plugin de Compose
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.json)
                implementation(libs.ktor.client.serialization)
                implementation(libs.androidx.activity.ktx)
                implementation(libs.androidx.material)
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.activity.compose)
                implementation(libs.androidx.runtime.android)
                implementation(libs.androidx.runtime)
                implementation(libs.androidx.compose.runtime)
                implementation(libs.kotlin.logging)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "com.example.shufflerionmultiplatform"
    compileSdk = 35
    defaultConfig {
        minSdk = 25
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(files("src/libs/spotify-app-remote-release-0.8.0.aar"))
    implementation(files("src/libs/spotify-auth-release-2.1.0.aar"))
    implementation(libs.ktor.client.cio)
    implementation(libs.gson)
    implementation(libs.androidx.media3.common.ktx)

}

