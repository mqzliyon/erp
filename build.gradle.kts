// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}

// Removed allprojects { repositories { ... } } and any repositories blocks as required by Gradle 7+ best practices.