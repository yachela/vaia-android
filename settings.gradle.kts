pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.24" apply false
        id("com.android.library") version "8.13.2" apply false
        id("androidx.navigation.safeargs.kotlin") version "2.7.5" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
        id("com.google.dagger.hilt.android") version "2.51.1" apply false
        id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
        id("com.google.gms.google-services") version "4.4.2" apply false
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
