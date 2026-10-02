plugins {
    id("com.android.application")
}

android {
    namespace = "dev.materialyou.gboardtheme"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.materialyou.gboardtheme"
        minSdk = 31
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sign with the debug key so the APK installs as-is; it is only an Xposed module.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly(project(":xposed-stub"))
    testImplementation("junit:junit:4.13.2")
}
