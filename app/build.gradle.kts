import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
val secrets = Properties().apply { rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
fun config(key: String) = System.getenv(key) ?: secrets.getProperty(key, "")
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
android {
    namespace = "hk.whatoeat"
    compileSdk = 36
    defaultConfig {
        applicationId = "hk.whatoeat"
        minSdk = 26
        targetSdk = 36
        buildConfigField("String", "SUPABASE_URL", quoted(config("SUPABASE_URL")))
        buildConfigField("String", "SUPABASE_PUBLIC_KEY", quoted(config("SUPABASE_PUBLIC_KEY")))
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "hk.whatoeat.EatTestRunner"
        buildConfigField("String", "PRIVACY_POLICY_URL", quoted(config("PRIVACY_POLICY_URL")))
        buildConfigField("String", "TERMS_URL", quoted(config("TERMS_URL")))
    }
    flavorDimensions += "data"
    productFlavors {
        create("demo") { dimension = "data"; applicationIdSuffix = ".demo"; buildConfigField("boolean", "DEMO", "true") }
        create("live") { dimension = "data"; buildConfigField("boolean", "DEMO", "false") }
    }
    buildTypes {
        debug {}
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
configurations.all {
    resolutionStrategy.force("androidx.concurrent:concurrent-futures:1.3.0")
}
dependencies {
    constraints {
        implementation("androidx.concurrent:concurrent-futures:1.3.0") {
            because("androidx.concurrent:concurrent-futures 1.2.0 is not available in repositories")
        }
    }
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui); implementation(libs.compose.material3); implementation(libs.compose.icons)
    implementation(libs.activity); implementation(libs.core)
    implementation(libs.lifecycle.runtime); implementation(libs.lifecycle.viewmodel); implementation(libs.navigation)
    implementation(libs.hilt); ksp(libs.hilt.compiler)
    implementation(libs.room); implementation(libs.room.ktx); ksp(libs.room.compiler)
    implementation(libs.datastore); implementation(libs.coroutines); implementation(libs.coroutines.tasks)
    implementation(libs.ads); implementation(libs.ump); implementation(libs.location); implementation(libs.material)
    // Match Room migration serializers in both target APK and instrumentation APK.
    implementation(libs.serialization.json)
    testImplementation(libs.junit); testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom)); androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.junit); androidTestImplementation(libs.test.runner); androidTestImplementation(libs.room.test)
    androidTestImplementation(libs.test.espresso)
    debugImplementation(libs.compose.tooling); debugImplementation(libs.compose.test.manifest)
}

androidComponents.onVariants { variant ->
    val testAds = variant.buildType == "debug" || variant.productFlavors.any { it.second == "demo" }
    val appId = if(testAds) config("ADMOB_CONSENT_TEST_APP_ID").ifBlank { "ca-app-pub-3940256099942544~3347511713" } else config("ADMOB_APP_ID")
    val banner = if(testAds) "ca-app-pub-3940256099942544/6300978111" else config("ADMOB_BANNER_ID")
    val open = if(testAds) "ca-app-pub-3940256099942544/9257395921" else config("ADMOB_APP_OPEN_ID")
    variant.manifestPlaceholders.put("admobAppId", appId.ifBlank { "ca-app-pub-3940256099942544~3347511713" })
    variant.buildConfigFields?.put("ADS_ENABLED", com.android.build.api.variant.BuildConfigField("boolean", (appId.isNotBlank() && banner.isNotBlank() && open.isNotBlank()).toString(), ""))
    variant.buildConfigFields?.put("ADMOB_BANNER_ID", com.android.build.api.variant.BuildConfigField("String", quoted(banner), ""))
    variant.buildConfigFields?.put("ADMOB_APP_OPEN_ID", com.android.build.api.variant.BuildConfigField("String", quoted(open), ""))
}
