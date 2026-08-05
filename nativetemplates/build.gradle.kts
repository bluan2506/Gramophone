plugins {
    id("com.android.library")
}

android {
    namespace = "com.google.android.ads.nativetemplates"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        lintConfig = file("../app/lint.xml")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    enableKotlin = false

    resourcePrefix = "gnt_"
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.1.0")
    implementation("com.google.android.material:material:1.13.0")
}
