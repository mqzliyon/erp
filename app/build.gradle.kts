plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    id("com.google.gms.google-services")
}



android {
    namespace = "com.dazzling.erp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dazzling.erp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Enable multidex for large app
        multiDexEnabled = true
        
        // Add build config fields for error handling
        buildConfigField("boolean", "ENABLE_CRASH_REPORTING", "true")
        buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "false")
        buildConfigField("String", "APP_VERSION", "\"1.0\"")
        
        // Add manifest placeholders for device-specific handling
        manifestPlaceholders["enableDeviceSpecificFixes"] = "true"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Suppress verbose logging in release
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "false")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            // Enable verbose logging only in debug
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    
    // Add packaging options to handle conflicts
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
    
    // Add lint options to suppress warnings
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "MissingTranslation"
        disable += "ExtraTranslation"
        disable += "UnusedResources"
        disable += "ObsoleteLintCustomCheck"
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.multidex:multidex:2.0.1")
    
    // Firebase - Use BOM to manage versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    
    // Google Play Services - Use compatible versions
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.fragment)
    annotationProcessor(libs.room.compiler)
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    
    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.9.0")
    
    // UI Components
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)
    
    // Charts
    implementation(libs.mpandroidchart)
    
    // Image Loading
    implementation(libs.glide)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Performance monitoring
    implementation("androidx.tracing:tracing:1.2.0")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.4.0")
    
    // PDF Generation
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}