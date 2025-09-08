import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    kotlin("plugin.serialization") version "2.2.10"
}


val properties = Properties().apply {
    load(FileInputStream(rootProject.file("local.properties")))
}
// 안전하게 키 읽기 (값이 없으면 에러를 띄우도록 하거나 기본값 설정)
val SUPABASE_ANON_KEY: String = properties.getProperty("SUPABASE_ANON_KEY")
    ?: throw GradleException("SUPABASE_ANON_KEY is not defined in local.properties")
val SUPABASE_URL: String = properties.getProperty("SUPABASE_URL")
    ?: throw GradleException("SUPABASE_URL is not defined in local.properties")


android {
    namespace = "com.malrang.pomodoro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.malrang.pomodoro"
        minSdk = 28
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_ANON_KEY", SUPABASE_ANON_KEY)
        buildConfigField("String", "SUPABASE_URL", SUPABASE_URL)

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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
//    implementation("com.himanshoe:charty:2.1.0-beta03.4") 멀티라인차트가 안되어 취소
    implementation ("io.github.ehsannarmani:compose-charts:0.1.9")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation(libs.firebase.crashlytics)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation(libs.androidx.animation.core)

    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.2"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:serializer-jackson-android:3.2.2")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    implementation("io.ktor:ktor-client-okhttp:3.2.2")
    implementation("io.ktor:ktor-client-core:3.2.2")
    implementation("io.ktor:ktor-client-android:3.2.2")
    implementation(libs.androidx.foundation)

    implementation("androidx.navigation:navigation-compose:2.9.3")

    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}