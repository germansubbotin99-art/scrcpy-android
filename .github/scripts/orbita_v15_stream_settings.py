from pathlib import Path
import re

STREAM = Path("app/src/main/java/tech/devline/scropy_ui/StreamActivity.kt")
BUILD = Path("app/build.gradle.kts")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Не найден фрагмент для v1.5: {label}")
    return text.replace(old, new, 1)


stream = STREAM.read_text(encoding="utf-8")

# Состояние качества трансляции. Значения сохраняются между запусками.
stream = replace_once(
    stream,
    '    private var audioMuted = mutableStateOf(false)\n',
    '    private var audioMuted = mutableStateOf(false)\n'
    '    private var streamFps = mutableStateOf(DEFAULT_MAX_FPS)\n'
    '    private var streamBitRate = mutableStateOf(DEFAULT_VIDEO_BIT_RATE)\n',
    "stream settings state",
)

# Загружаем сохранённые настройки до первого подключения.
stream = replace_once(
    stream,
    '        super.onCreate(savedInstanceState)\n'
    '        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n',
    '        super.onCreate(savedInstanceState)\n'
    '        val streamPrefs = getSharedPreferences("orbita_stream_settings", android.content.Context.MODE_PRIVATE)\n'
    '        streamFps.value = streamPrefs.getInt("fps", DEFAULT_MAX_FPS).let { if (it == 60) 60 else 30 }\n'
    '        streamBitRate.value = when (streamPrefs.getInt("bitrate", DEFAULT_VIDEO_BIT_RATE)) {\n'
    '            2_000_000 -> 2_000_000\n'
    '            8_000_000 -> 8_000_000\n'
    '            12_000_000 -> 12_000_000\n'
    '            else -> 4_000_000\n'
    '        }\n'
    '        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n',
    "load stream prefs",
)

# Передаём текущие значения и обработчик применения в Compose.
stream = replace_once(
    stream,
    '                    audioMuted = audioMuted.value,\n'
    '                    onToggleScreen = {\n',
    '                    audioMuted = audioMuted.value,\n'
    '                    streamFps = streamFps.value,\n'
    '                    streamBitRate = streamBitRate.value,\n'
    '                    onApplyStreamSettings = { fps, bitRate ->\n'
    '                        val changed = fps != streamFps.value || bitRate != streamBitRate.value\n'
    '                        streamFps.value = fps\n'
    '                        streamBitRate.value = bitRate\n'
    '                        getSharedPreferences("orbita_stream_settings", android.content.Context.MODE_PRIVATE)\n'
    '                            .edit()\n'
    '                            .putInt("fps", fps)\n'
    '                            .putInt("bitrate", bitRate)\n'
    '                            .apply()\n'
    '                        if (changed) {\n'
    '                            disconnectAll()\n'
    '                            errorText.value = null\n'
    '                            statusText.value = "Применение качества: ${fps} FPS • ${bitRate / 1_000_000} Мбит/с…"\n'
    '                            isConnected.value = false\n'
    '                            val u = pendingUsbDevice\n'
    '                            if (u != null) connectUsbIfReady(u)\n'
    '                            else pendingHost?.let { connectIfReady(it, pendingPort) }\n'
    '                        }\n'
    '                    },\n'
    '                    onToggleScreen = {\n',
    "compose settings callback",
)

# Scrcpy получает выбранные значения вместо жёстко заданных 30 FPS / 4 Мбит.
if stream.count('maxFps = DEFAULT_MAX_FPS,') == 2:
    stream = stream.replace(
        'maxFps = DEFAULT_MAX_FPS,',
        'maxFps = streamFps.value,'
    )

if stream.count('videoBitRate = DEFAULT_VIDEO_BIT_RATE,') == 2:
    stream = stream.replace(
        'videoBitRate = DEFAULT_VIDEO_BIT_RATE,',
        'videoBitRate = streamBitRate.value'
    )
# Новые параметры StreamScreen.
stream = replace_once(
    stream,
    '    audioMuted: Boolean,\n'
    '    onToggleScreen: () -> Unit,\n',
    '    audioMuted: Boolean,\n'
    '    streamFps: Int,\n'
    '    streamBitRate: Int,\n'
    '    onApplyStreamSettings: (Int, Int) -> Unit,\n'
    '    onToggleScreen: () -> Unit,\n',
    "StreamScreen signature",
)

# Динамический HUD и передача настроек в плавающее меню.
stream = replace_once(
    stream,
    '        if (isConnected) {\n'
    '            FloatingControls(\n'
    '                screenOn = screenOn,\n'
    '                audioMuted = audioMuted,\n',
    '        if (isConnected) {\n'
    '            OrbitaHud(statusText, streamFps, streamBitRate)\n'
    '            FloatingControls(\n'
    '                screenOn = screenOn,\n'
    '                audioMuted = audioMuted,\n'
    '                streamFps = streamFps,\n'
    '                streamBitRate = streamBitRate,\n'
    '                onApplyStreamSettings = onApplyStreamSettings,\n',
    "connected controls",
)

# Новые параметры FloatingControls.
stream = replace_once(
    stream,
    '    audioMuted: Boolean,\n'
    '    onBack: () -> Unit,\n',
    '    audioMuted: Boolean,\n'
    '    streamFps: Int,\n'
    '    streamBitRate: Int,\n'
    '    onApplyStreamSettings: (Int, Int) -> Unit,\n'
    '    onBack: () -> Unit,\n',
    "FloatingControls signature",
)

# Черновые значения в меню: пользователь выбирает их без немедленного разрыва связи.
stream = replace_once(
    stream,
    '    var offset by remember { mutableStateOf(Offset(-marginPx, marginPx)) }\n'
    '    var expanded by remember { mutableStateOf(false) }\n',
    '    var offset by remember { mutableStateOf(Offset(-marginPx, marginPx)) }\n'
    '    var expanded by remember { mutableStateOf(false) }\n'
    '    var showQuality by remember { mutableStateOf(false) }\n'
    '    var selectedFps by remember(streamFps) { mutableStateOf(streamFps) }\n'
    '    var selectedBitRate by remember(streamBitRate) { mutableStateOf(streamBitRate) }\n',
    "quality menu state",
)

# Компактный блок качества в плавающем меню.
stream = replace_once(
    stream,
    '                MenuItem(if (screenOn) "Выключить экран устройства" else "Включить экран устройства", onToggleScreen)\n'
    '                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 6.dp))\n'
    '                MenuItem("Отключиться", onDisconnect, danger = true)\n',
    '                MenuItem(if (screenOn) "Выключить экран устройства" else "Включить экран устройства", onToggleScreen)\n'
    '                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 6.dp))\n'
    '                MenuItem(\n'
    '                    "Качество: ${streamFps} FPS • ${streamBitRate / 1_000_000} Мбит/с",\n'
    '                    { showQuality = !showQuality },\n'
    '                )\n'
    '                if (showQuality) {\n'
    '                    MenuItem("FPS: $selectedFps  • нажми для смены", {\n'
    '                        selectedFps = if (selectedFps == 30) 60 else 30\n'
    '                    })\n'
    '                    MenuItem("Битрейт: ${selectedBitRate / 1_000_000} Мбит/с  • нажми для смены", {\n'
    '                        selectedBitRate = when (selectedBitRate) {\n'
    '                            2_000_000 -> 4_000_000\n'
    '                            4_000_000 -> 8_000_000\n'
    '                            8_000_000 -> 12_000_000\n'
    '                            else -> 2_000_000\n'
    '                        }\n'
    '                    })\n'
    '                    if (selectedFps != streamFps || selectedBitRate != streamBitRate) {\n'
    '                        MenuItem("↻ Применить и переподключить", {\n'
    '                            showQuality = false\n'
    '                            onApplyStreamSettings(selectedFps, selectedBitRate)\n'
    '                        })\n'
    '                    }\n'
    '                }\n'
    '                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 6.dp))\n'
    '                MenuItem("Отключиться", onDisconnect, danger = true)\n',
    "quality controls",
)

# Собственный динамический HUD. Его наличие также не даёт старой Gradle-задаче
# вставить фиксированную надпись «30 FPS».
hud = r'''
@Composable
private fun BoxScope.OrbitaHud(status: String, fps: Int, bitRate: Int) {
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
            "$fps FPS • ${bitRate / 1_000_000} Мбит/с • LOW LOAD",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color(0xFFB88CFF),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

'''
stream = replace_once(
    stream,
    '@Composable\nprivate fun RowScope.QuickButton',
    hud + '@Composable\nprivate fun RowScope.QuickButton',
    "dynamic Orbita HUD",
)

STREAM.write_text(stream, encoding="utf-8")

# Маркируем собранный APK как v1.5.0. Это не меняет applicationId,
# поэтому приложение обновляется поверх проверенной версии.
build = BUILD.read_text(encoding="utf-8")
build, version_code_count = re.subn(r'versionCode\s*=\s*\d+', 'versionCode = 4', build, count=1)
build, version_name_count = re.subn(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.5.0"', build, count=1)
if version_code_count != 1 or version_name_count != 1:
    raise RuntimeError("Не удалось обновить versionCode/versionName для v1.5")
build = build.replace("STITCHLINK • ОРБИТА v1.2", "STITCHLINK • ОРБИТА v1.5")
BUILD.write_text(build, encoding="utf-8")

print("ORBITA v1.5 stream settings applied")
