@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.philburk")
            }
        }
        // TopOn / AnyThink ad SDK (com.thinkup.sdk, com.smartdigimkttech.sdk)
        maven("https://jfrog.anythinktech.com/artifactory/overseas_sdk")

        // --- Ad-mediation network repos (Vungle + Unity Ads resolve from mavenCentral above) ---
        // 3. Pangle
        maven("https://artifact.bytedance.com/repository/pangle")
        // 9. ironSource
        maven("https://android-sdk.is.com/")
        // 10. Chartboost
        maven("https://cboost.jfrog.io/artifactory/chartboost-ads")
        maven("https://cboost.jfrog.io/artifactory/chartboost-mediation")
        maven("https://cboost.jfrog.io/artifactory/chartboost-core")
        // 12. Mintegral
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        // 17. Appnext
        maven("https://dl.appnext.com")
    }
}

rootProject.name = "Gramophone"
includeBuild(file("media3").toPath().toRealPath().toAbsolutePath().toString()) {
    dependencySubstitution {
        substitute(module("androidx.media3:media3-common")).using(project(":lib-common"))
        substitute(module("androidx.media3:media3-common-ktx")).using(project(":lib-common-ktx"))
        substitute(module("androidx.media3:media3-exoplayer")).using(project(":lib-exoplayer"))
        substitute(module("androidx.media3:media3-exoplayer-midi")).using(project(":lib-decoder-midi"))
        substitute(module("androidx.media3:media3-session")).using(project(":lib-session"))
        // Chartboost mediation pulls media3-ui — map it to the local fork too so all media3 modules
        // stay on the same version instead of dragging a mismatched media3-ui from Maven.
        substitute(module("androidx.media3:media3-ui")).using(project(":lib-ui"))
    }
}

include(":misc:audiofxstub")
include(":misc:audiofxstub2")
include(":misc:audiofxfwd")
include(":misc:alacdecoder")
include(":nativetemplates")
include(":hificore")
include(":app")
include(":baselineprofile")
