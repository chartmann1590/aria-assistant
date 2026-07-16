import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.exists()) {
        releaseKeystorePropertiesFile.inputStream().use { load(it) }
    }
}

val releaseVersionCode = providers.environmentVariable("ANDROID_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
    ?: 1
val releaseVersionName = providers.environmentVariable("ANDROID_VERSION_NAME")
    .orNull
    ?.takeIf { it.isNotBlank() }
    ?: "1.0.0"

android {
    namespace = "com.aria.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aria.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = rootProject.file("local.properties")
        val debugEnabled = if (localProps.exists()) {
            localProps.useLines { lines ->
                lines.find { it.trimStart().startsWith("aria.debug") }
                    ?.substringAfter("=")?.trim()?.toBooleanStrictOrNull() ?: false
            }
        } else false
        buildConfigField("boolean", "DEBUG_MODE", debugEnabled.toString())

        fun readProp(name: String): String {
            if (localProps.exists()) {
                localProps.useLines { lines ->
                    lines.find { it.trimStart().startsWith(name) }
                        ?.substringAfter("=")?.trim()?.takeIf { it.isNotEmpty() }
                        ?.let { return it }
                }
            }
            return project.findProperty(name) as? String ?: ""
        }

        val githubApiToken = readProp("github.api.token")
        val githubRepoOwner = readProp("github.repo.owner")
        val githubRepoName = readProp("github.repo.name")

        buildConfigField("String", "GITHUB_API_TOKEN", "\"${githubApiToken}\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"${githubRepoOwner}\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"${githubRepoName}\"")
        buildConfigField("String", "FEEDBACK_ASSETS_DIR", "\"feedback-assets\"")

        // AdMob — falls back to Google's public test IDs so a checkout without
        // local.properties still builds and shows test ads instead of failing.
        val admobAppId = readProp("admob.app.id").ifBlank { "ca-app-pub-3940256099942544~3347511713" }
        val admobBannerId = readProp("admob.banner.id").ifBlank { "ca-app-pub-3940256099942544/6300978111" }
        val admobInterstitialId = readProp("admob.interstitial.id").ifBlank { "ca-app-pub-3940256099942544/1033173712" }

        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"${admobBannerId}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_AD_UNIT_ID", "\"${admobInterstitialId}\"")
        manifestPlaceholders["adMobAppId"] = admobAppId
    }

    signingConfigs {
        if (releaseKeystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("META-INF/{AL2.0,LGPL2.1}")
        jniLibs.pickFirsts += setOf("**/libonnxruntime.so")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.openwakeword)
    implementation(libs.onnxruntime.android)
    implementation(libs.sherpa.onnx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.billing.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.activity.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.litertlm.android)
    implementation(libs.play.services.location)
    implementation(libs.play.services.ads)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.json:json:20231013")
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation("androidx.test.ext:junit:1.1.5")

    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}