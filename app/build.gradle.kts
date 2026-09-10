import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mebleech.probe"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oscargines.pruebameb"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(localProperties.getProperty("meb.upload.storeFile"))
            storePassword = localProperties.getProperty("meb.upload.storePassword")
            keyAlias = localProperties.getProperty("meb.upload.keyAlias")
            keyPassword = localProperties.getProperty("meb.upload.keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.car.app.projected)
}
