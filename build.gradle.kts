buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.

task("clean") {
    delete(layout.buildDirectory)
}
