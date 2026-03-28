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
        // JitPack als Backup, falls FFmpeg oder andere Tools dort gesucht werden
        maven { url = uri("https://jitpack.io") }
      //  maven { url = uri("https://artifactory.appodeal.com/appodeal-public/") }
    }
}

rootProject.name = "WA Status Video Cutter"
include(":app")