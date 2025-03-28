import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    alias(libs.plugins.newrelic)
    kotlin("plugin.serialization") version "2.1.0"
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

//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "shared"
//            isStatic = true
//        }
//    }

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
                implementation(libs.navigation.compose)
                implementation(libs.ktor.client.websockets)
                implementation(libs.kotlinx.serialization.json)
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
        minSdk = 26
        manifestPlaceholders["redirectHostName"] = "shufflerionApp"
        manifestPlaceholders["redirectSchemeName"] = "callback"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    api(project(":spotify-app-remote"))
    api(project(":spotify-auth"))
    implementation(libs.ktor.client.cio)
    implementation(libs.gson)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.precompose)
    implementation(libs.image.loader)
    implementation(libs.android.agent)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.ktor.client.json)
    implementation(libs.ktor.serialization.kotlinx.json)
}
