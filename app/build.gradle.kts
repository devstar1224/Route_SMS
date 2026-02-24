plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "com.routesms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.routesms"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Debug 빌드에서도 OSS 라이선스 데이터가 보이도록
// release 태스크 출력을 debug에 복사
afterEvaluate {
    tasks.named("debugOssLicensesTask") {
        dependsOn("releaseOssLicensesTask")
        doLast {
            val releaseDir = file("$buildDir/generated/third_party_licenses/release/res/raw")
            val debugDir = file("$buildDir/generated/third_party_licenses/debug/res/raw")
            if (releaseDir.exists()) {
                releaseDir.listFiles()?.forEach { file ->
                    file.copyTo(debugDir.resolve(file.name), overwrite = true)
                }
            }
        }
    }
}

dependencies {
    // Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // DataStore
    implementation(libs.datastore.preferences)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // AdMob
    implementation(libs.google.mobile.ads)

    // OSS Licenses
    implementation(libs.oss.licenses)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
}
