plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.cyberdyne.markdown.wear"
    compileSdk = 34
    publishing { singleVariant("release") }
    defaultConfig { minSdk = 26 }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    api(project(":markdown-engine"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.cyberdyne.markdown"
                artifactId = "markdown-engine-wear"
                version = "1.0.0"
            }
        }
    }
}
