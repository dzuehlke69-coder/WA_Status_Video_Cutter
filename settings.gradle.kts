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
        mavenCentral() // <--- Muss unbedingt VOR JitPack stehen
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "WA Status Video Cutter"
include(":app")