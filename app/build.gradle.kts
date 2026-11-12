plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.kichikan.negah"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.kichikan.negah"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}
kapt { correctErrorTypes = true }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":domain:core"))
    implementation(project(":feature:home"))
    implementation(project(":feature:members"))
    implementation(project(":feature:refinery"))
    implementation(project(":data:local"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
