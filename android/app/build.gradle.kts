import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val brand =
    Properties().apply {
        rootProject.file("../shared/brand.properties").reader(Charsets.UTF_8).use { load(it) }
    }

val releaseVersion = Properties().apply {
    rootProject.file("../shared/release.properties").reader(Charsets.UTF_8).use { load(it) }
}

android {
    namespace = "app.onepunch"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.onepunch"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersion.getProperty("androidVersionCode").toInt()
        versionName = releaseVersion.getProperty("version")
        resValue("string", "app_name", brand.getProperty("displayName"))
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets["main"].assets.srcDir("../../shared")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
