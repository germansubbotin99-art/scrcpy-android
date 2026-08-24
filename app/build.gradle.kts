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

// STITCHLINK v1.2 branding layer. Upstream keeps many Compose labels directly
// in MainActivity.kt, so we localise and restyle that screen before compilation
// without touching its USB/ADB connection logic.
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

        val oldHome = """
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STITCHLINK • ОРБИТА") },
                actions = { TextButton(onClick = onNewConnection) { Text("+ Новое подключение") } },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Сохранённых устройств пока нет",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewConnection) { Text("+ Подключить устройство") }
                }
            }
        } else {
""".trimIndent()

        val newHome = """
    Scaffold(
        containerColor = Color(0xFF070B22),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070B22),
                    titleContentColor = Color(0xFFF1F5FF),
                    actionIconContentColor = Color(0xFF35C8FF),
                ),
                title = {
                    Column {
                        Text(
                            "STITCHLINK",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF8BE4FF),
                        )
                        Text(
                            "ОРБИТА • Android Link",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAB6DC),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNewConnection) {
                        Text("+ Подключить", color = Color(0xFFE34DFF))
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF070B22))
        ) {
        if (devices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    color = Color(0xFF12183A),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth(0.78f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 30.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "◉",
                            fontSize = 54.sp,
                            color = Color(0xFF8B5CFF),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "ОРБИТА ГОТОВА",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFF1F5FF),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Подключи Android-устройство по USB / OTG или Wi‑Fi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFAAB6DC),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "USB/OTG • ожидание устройства",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF35C8FF),
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onNewConnection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CFF),
                                contentColor = Color.White,
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("＋ ПОДКЛЮЧИТЬ УСТРОЙСТВО")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "STITCHLINK • ОРБИТА v1.2",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6977A8),
                        )
                    }
                }
            }
        } else {
""".trimIndent()

        if (text.contains(oldHome)) {
            text = text.replace(oldHome, newHome)
        }

        source.writeText(text)
    }
}

// ORBITA HUD is intentionally local-only: no extra ADB polling, no telemetry
// requests, and no additional video settings are sent to the controller.
tasks.register("injectOrbitaHud") {
    doLast {
        val source = file("src/main/java/tech/devline/scropy_ui/StreamActivity.kt")
        var text = source.readText()
        val anchor = """        if (isConnected) {
            FloatingControls("""
        if (!text.contains("private fun BoxScope.OrbitaHud(")) {
            text = text.replace(
                anchor,
                """        if (isConnected) {
            OrbitaHud(statusText)
            FloatingControls("""
            )
            text += """

@Composable
private fun BoxScope.OrbitaHud(status: String) {
    Surface(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 14.dp, top = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x99101635),
        contentColor = Color.White,
        shadowElevation = 4.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("◉", color = Color(0xFF40DFFF), style = MaterialTheme.typography.titleMedium)
            Column {
                Text(
                    "ОРБИТА HUD",
                    color = Color(0xFF9EEBFF),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    status,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }

    Surface(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 14.dp, bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0x88101635),
        contentColor = Color.White,
    ) {
        Text(
            "USB • 30 FPS • LOW LOAD",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color(0xFFB88CFF),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
"""
            source.writeText(text)
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyScrcpyServer")
    dependsOn("brandUiSources")
    dependsOn("injectOrbitaHud")
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
