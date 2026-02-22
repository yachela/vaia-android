pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.10" apply false
        id("com.android.library") version "8.13.2" apply false
        id("androidx.navigation.safeargs.kotlin") version "2.7.5" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "vaia-android"
include(":app")
