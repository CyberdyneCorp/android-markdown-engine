plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.cyberdyne.markdown.codeblocks"
    compileSdk = 34
    publishing { singleVariant("release") }
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

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.cyberdyne.markdown"
                artifactId = "markdown-engine-codeblocks"
                version = "1.0.0"
            }
        }
    }
}
