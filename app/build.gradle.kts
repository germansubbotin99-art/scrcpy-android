import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProps = Properties().also { props ->
    val file = rootProject.file("keystore.properties")
    if (file.exists()) props.load(file.inputStream())
}

tasks.register<Copy>("copyScrcpyServer") {
    val serverApk = rootProject.file("../server/build/outputs/apk/release/server-release-unsigned.apk")
    if (serverApk.exists()) {
        from(serverApk)
        into("src/main/assets")
        rename { "scrcpy-server" }
    }
}

// STITCHLINK v1.2: translate the legacy hard-coded Compose labels before compile.
// The upstream project keeps most UI text directly in MainActivity.kt, so this
// build step lets us localise the app without touching its connection logic.
tasks.register("brandUiSources") {
    doLast {
        val source = file("src/main/java/tech/devline/scropy_ui/MainActivity.kt")
        var text = source.readText()
        val replacements = linkedMapOf(
            "\"Devices\"" to "\"STITCHLINK • ОРБИТА\"",
            "\"+ New\"" to "\"+ Новое подключение\"",
            "\"No saved devices yet.\"" to "\"Сохранённых устройств пока нет\"",
            "\"+ New Connection\"" to "\"+ Подключить устройство\"",
            "\"About\"" to "\"О приложении\"",
            "\"Edit\"" to "\"Изменить\"",
            "\"Connecting...\"" to "\"Подключение…\"",
            "\"Connection failed\"" to "\"Ошибка подключения\"",
            "\"Stream\"" to "\"Трансляция\"",
            "\"Shell\"" to "\"Терминал\"",
            "\"Edit connection\"" to "\"Изменить подключение\"",
            "\"Name\"" to "\"Название\"",
            "\"IP address\"" to "\"IP-адрес\"",
            "\"Port\"" to "\"Порт\"",
            "\"Save\"" to "\"Сохранить\"",
            "\"Cancel\"" to "\"Отмена\"",
            "\"New Connection\"" to "\"Новое подключение\"",
            "\"< Back\"" to "\"< Назад\"",
            "\"How do you want to connect?\"" to "\"Как подключить устройство?\"",
            "\"ADB over WiFi\"" to "\"ADB по Wi‑Fi\"",
            "\"ADB over USB\"" to "\"ADB по USB / OTG\"",
            "\"WiFi Connection\"" to "\"Подключение по Wi‑Fi\"",
            "\"What do you want to do?\"" to "\"Выберите режим\"",
            "\"Device IP address\"" to "\"IP-адрес устройства\"",
            "\"USB Connection\"" to "\"Подключение по USB\"",
            "\"Retry\"" to "\"Повторить\"",
            "\"Go Back\"" to "\"Назад\"",
            "\"OK\"" to "\"Готово\""
        )
        replacements.forEach { (from, to) -> text = text.replace(from, to) }
        source.writeText(text)
    }
}

tasks.named("preBuild") {
    dependsOn("copyScrcpyServer")
    dependsOn("brandUiSources")
}

android {
    namespace = "tech.devline.scropy_ui"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "tech.devline.scropy_ui"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2-stitchlink-orbita"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("storeFile", "")
            if (storeFilePath.isNotEmpty()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
