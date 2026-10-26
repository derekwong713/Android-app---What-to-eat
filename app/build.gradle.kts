import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
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
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PRIVACY_POLICY_URL", quoted(config("PRIVACY_POLICY_URL")))
        buildConfigField("String", "TERMS_URL", quoted(config("TERMS_URL")))
    }
    flavorDimensions += "data"
    productFlavors {
        create("demo") { dimension = "data"; applicationIdSuffix = ".demo"; buildConfigField("boolean", "DEMO", "true") }
        create("live") { dimension = "data"; buildConfigField("boolean", "DEMO", "false") }
    }
    buildTypes {
        debug { buildConfigField("String", "PLACES_API_KEY", quoted(config("PLACES_API_KEY_DEBUG"))) }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "PLACES_API_KEY", quoted(config("PLACES_API_KEY_RELEASE")))
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kapt { correctErrorTypes = true; arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui); implementation(libs.compose.material3); implementation(libs.compose.icons)
    implementation(libs.activity); implementation(libs.core)
    implementation(libs.lifecycle.runtime); implementation(libs.lifecycle.viewmodel); implementation(libs.navigation)
    implementation(libs.hilt); kapt(libs.hilt.compiler)
    implementation(libs.room); implementation(libs.room.ktx); kapt(libs.room.compiler)
    implementation(libs.datastore); implementation(libs.coroutines); implementation(libs.coroutines.tasks)
    implementation(libs.places); implementation(libs.location); implementation(libs.material)
    testImplementation(libs.junit); testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom)); androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.junit); androidTestImplementation(libs.test.runner); androidTestImplementation(libs.room.test)
    debugImplementation(libs.compose.tooling); debugImplementation(libs.compose.test.manifest)
}
