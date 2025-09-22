import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)

}

android {
    namespace = "com.example.winsrehab"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.example.winsrehab"
        minSdk = 26   // 建议别设太高，30 会导致很多设备装不了
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}
kapt {
    arguments {
        // schemas 文件夹可以自己改名字，放在 app 目录下即可
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}


dependencies {
    // Kotlin 标准库（改成 2.0.21 稳定版）
    implementation(libs.kotlin.stdlib)

    // Jetpack 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Activity KTX（提供 by viewModels() 扩展）
    implementation(libs.androidx.activity.ktx)
    //约束布局
    implementation(libs.androidx.constraintlayout)
    //RecycleView

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // 可选：RecyclerView 的选择支持
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    // CardView 核心依赖
    implementation("androidx.cardview:cardview:1.0.0")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)


    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
