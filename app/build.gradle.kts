plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "sprout.app.sakmvp1"
    compileSdk = 35

    defaultConfig {
        applicationId = "sprout.app.sakmvp1"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.enabled = false
        }
    }

    lint {
        disable.add("NewApi")  // SDK 버전 체크가 이미 되어 있는 경우 무시
    }
}

dependencies {

    implementation("androidx.cardview:cardview:1.0.0")

    // Firebase BOM (최신 버전)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    // Firebase 모듈 (버전 생략)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.github.bumptech.glide:glide:4.16.0")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Jsoup for HTML parsing (meal menu scraping)
    implementation("org.jsoup:jsoup:1.17.2")

    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // iText for PDF export
    implementation("com.itextpdf:itext7-core:7.2.5")

    // PhotoView for zoomable images
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.9.0")

    // Desugar library for Java 8+ API support on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation("org.osmdroid:osmdroid-android:6.1.18")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}