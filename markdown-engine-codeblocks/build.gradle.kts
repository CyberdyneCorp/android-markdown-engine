plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cyberdyne.markdown.codeblocks"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests { isReturnDefaultValues = true } }
}

dependencies {
    api(project(":markdown-engine"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.graphics)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
