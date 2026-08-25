from pathlib import Path
import re

MAIN = Path("app/src/main/java/tech/devline/scropy_ui/MainActivity.kt")
BUILD = Path("app/build.gradle.kts")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Не найден фрагмент v1.5.2: {label}")
    return text.replace(old, new, 1)


main = MAIN.read_text(encoding="utf-8")

main = replace_once(
    main,
    'private const val KEY_SAVED = "saved_devices_v2"\n',
    'private const val KEY_SAVED = "saved_devices_v2"\n'
    'private const val KEY_LAST_USED = "last_used_device_id"\n',
    "last used key",
)

main = replace_once(
    main,
    '    var editingDevice by remember { mutableStateOf<SavedDevice?>(null) }\n\n'
    '    Scaffold(\n',
    '    var editingDevice by remember { mutableStateOf<SavedDevice?>(null) }\n'
    '    val usbManager = remember(ctx) { ctx.getSystemService(Context.USB_SERVICE) as UsbManager }\n'
    '    val connectivityManager = remember(ctx) {\n'
    '        ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager\n'
    '    }\n'
    '    var usbReady by remember { mutableStateOf(false) }\n'
    '    var wifiReady by remember { mutableStateOf(false) }\n'
    '    LaunchedEffect(Unit) {\n'
    '        while (true) {\n'
    '            usbReady = findAdbDevices(usbManager).isNotEmpty()\n'
    '            val network = connectivityManager.activeNetwork\n'
    '            val caps = connectivityManager.getNetworkCapabilities(network)\n'
    '            wifiReady = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true\n'
    '            kotlinx.coroutines.delay(1500)\n'
    '        }\n'
    '    }\n'
    '    val lastUsedId = prefs.getString(KEY_LAST_USED, null)\n'
    '    val quickDevice = devices.firstOrNull { it.id == lastUsedId } ?: devices.firstOrNull()\n\n'
    '    Scaffold(\n',
    "operator status state",
)

main = replace_once(
    main,
    '                                                            connectingId = null; onLaunchStream(device.host, livePort)\n',
    '                                                            prefs.edit().putString(KEY_LAST_USED, device.id).apply()\n'
    '                                                            connectingId = null; onLaunchStream(device.host, livePort)\n',
    "remember last streamed device",
)

main = main.replace(
    'contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 60.dp),',
    'contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),',
    1,
)

about_anchor = '''        TextButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
'''

operator_bar = '''        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color(0xE6121736),
            contentColor = Color.White,
            shadowElevation = 8.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    if (usbReady) "USB ●" else "USB ○",
                    color = if (usbReady) Color(0xFF40DFFF) else Color(0xFF6977A8),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    if (wifiReady) "Wi‑Fi ●" else "Wi‑Fi ○",
                    color = if (wifiReady) Color(0xFF8BE4FF) else Color(0xFF6977A8),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    "ADB • ${devices.size}",
                    color = if (devices.isNotEmpty()) Color(0xFFB88CFF) else Color(0xFF6977A8),
                    style = MaterialTheme.typography.labelSmall,
                )
                quickDevice?.let { quick ->
                    TextButton(
                        enabled = connectingId == null,
                        onClick = {
                            if (connectingId == null) {
                                connectingId = quick.id
                                errorMsg = null
                                scope.launch {
                                    val livePort = resolveAdbPortViaMdns(ctx, quick.host, quick.port)
                                    if (livePort != quick.port) {
                                        updateDeviceById(prefs, quick.copy(port = livePort))
                                        devices = loadDevices(prefs)
                                    }
                                    prefs.edit().putString(KEY_LAST_USED, quick.id).apply()
                                    connectingId = null
                                    onLaunchStream(quick.host, livePort)
                                }
                            }
                        },
                    ) {
                        Text("▶ Быстро", maxLines = 1, color = Color(0xFFE34DFF))
                    }
                }
            }
        }

        TextButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp),
'''

main = replace_once(main, about_anchor, operator_bar, "operator bottom bar")
MAIN.write_text(main, encoding="utf-8")

build = BUILD.read_text(encoding="utf-8")
build, version_code_count = re.subn(r'versionCode\s*=\s*\d+', 'versionCode = 6', build, count=1)
build, version_name_count = re.subn(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.5.2"', build, count=1)
if version_code_count != 1 or version_name_count != 1:
    raise RuntimeError("Не удалось обновить версию для v1.5.2")
build = build.replace("STITCHLINK • ОРБИТА v1.5.1", "STITCHLINK • ОРБИТА v1.5.2")
BUILD.write_text(build, encoding="utf-8")

print("ORBITA v1.5.2 quick connect and status applied")
