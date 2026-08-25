from pathlib import Path
import re

MAIN = Path("app/src/main/java/tech/devline/scropy_ui/MainActivity.kt")
STREAM = Path("app/src/main/java/tech/devline/scropy_ui/StreamActivity.kt")


def apply_replacements(path: Path, replacements: dict[str, str]) -> None:
    text = path.read_text(encoding="utf-8")
    for old, new in replacements.items():
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


common = {
    "import tech.devline.scropy_ui.ui.theme.ScropyTheme": "import tech.devline.scropy_ui.ui.theme.OrbitaTheme",
    "ScropyTheme {": "OrbitaTheme {",
}

main_replacements = {
    **common,
    'title = { Text("Devices") }': 'title = { Text("STITCHLINK • ОРБИТА") }',
    'Text("+ New")': 'Text("+ Добавить")',
    '"No saved devices yet."': '"Нет подключённых устройств"',
    'Text("+ New Connection")': 'Text("+ Новое подключение")',
    'contentDescription = "Last screenshot"': 'contentDescription = "Последний снимок"',
    'Text("Edit")': 'Text("Изменить")',
    'Text("Connecting...")': 'Text("Подключение...")',
    '"Connection failed"': '"Ошибка подключения"',
    'Text("Stream")': 'Text("Трансляция")',
    'Text("Shell")': 'Text("Терминал")',
    'Text("About")': 'Text("О приложении")',
    'title = { Text("Edit connection") }': 'title = { Text("Изменить подключение") }',
    'label = { Text("Name") }': 'label = { Text("Название") }',
    'label = { Text("IP address") }': 'label = { Text("IP-адрес") }',
    'label = { Text("Port") }': 'label = { Text("Порт") }',
    '"Tip: the port updates itself automatically on connect, so you rarely need to set it here."': '"Порт обновляется автоматически при подключении, поэтому вручную менять его обычно не требуется."',
    'Text("Save")': 'Text("Сохранить")',
    'Text("Cancel")': 'Text("Отмена")',
    'title = { Text("About Scropy Android") }': 'title = { Text("О STITCHLINK • ОРБИТА") }',
    'Text("OK")': 'Text("Закрыть")',
    'title = { Text("New Connection") }': 'title = { Text("Новое подключение") }',
    'Text("< Back")': 'Text("Назад")',
    'Text("How do you want to connect?")': 'Text("Выберите способ подключения")',
    'Text("ADB over WiFi")': 'Text("Беспроводное подключение")',
    '"Connect wirelessly via IP address.\\nRequires Wireless Debugging enabled."': '"Подключение по Wi‑Fi через IP-адрес.\\nТребуется включённая беспроводная отладка."',
    'Text("ADB over USB")': 'Text("USB OTG")',
    '"Connect via USB OTG cable.\\nRequires USB Debugging enabled."': '"Подключение через кабель USB OTG.\\nТребуется включённая USB-отладка."',
    'title = { Text("WiFi Connection") }': 'title = { Text("Беспроводное подключение") }',
    'Text("What do you want to do?")': 'Text("Выберите действие")',
    'label = { Text("Device IP address") }': 'label = { Text("IP-адрес устройства") }',
    'placeholder = { Text("e.g. 192.168.1.65") }': 'placeholder = { Text("например 192.168.1.65") }',
    'label = { Text("ADB port (shown under Wireless debugging)") }': 'label = { Text("ADB-порт из раздела «Беспроводная отладка»") }',
    'placeholder = { Text("e.g. 38765") }': 'placeholder = { Text("например 38765") }',
    'connecting = true; status = "Connecting..."': 'connecting = true; status = "Подключение..."',
    'connecting = false; status = "Getting device info..."': 'connecting = false; status = "Получение данных устройства..."',
    'Text(if (connecting) "Connecting..." else "Connect")': 'Text(if (connecting) "Подключение..." else "Подключить")',
    'Text(if (scanning || discovering) "Scanning..." else "Discover & scan network")': 'Text(if (scanning || discovering) "Поиск..." else "Найти устройства в сети")',
    '"Discovering devices on your network\\u2026"': '"Поиск устройств в локальной сети…"',
    '"Android Devices"': '"Android-устройства"',
    '"${device.host}  \\u00b7  port ${device.port}"': '"${device.host}  ·  порт ${device.port}"',
    'if (portScanning) "Scanning ports on $portScanHost..."': 'if (portScanning) "Поиск портов на $portScanHost..."',
    'else "Scan all ADB ports on $portScanHost"': 'else "Сканировать ADB-порты на $portScanHost"',
    '"Probing all 5-digit ports (10000–65535) in parallel…"': '"Проверка портов 10000–65535…"',
    '"Open ADB ports — tap to use:"': '"Открытые ADB-порты — нажмите для выбора:"',
    '"No open 5-digit ports found on $portScanHost."': '"Открытые ADB-порты на $portScanHost не найдены."',
    '"Tip: tap \\"Scan\\" to auto-discover ADB devices on your WiFi."': '"Нажмите «Найти устройства в сети» для автоматического поиска ADB-устройств."',
    'Text("Device not paired yet")': 'Text("Устройство ещё не сопряжено")',
    '"On device: Settings > Developer options > Wireless debugging\\n> \\"Pair device with pairing code\\""': '"На устройстве: Настройки → Для разработчиков → Беспроводная отладка\\n→ «Сопряжение с помощью кода»"',
    'label = { Text("Pairing port (shown on pairing dialog)") }': 'label = { Text("Порт сопряжения") }',
    'label = { Text("6-digit pairing code") }': 'label = { Text("6-значный код сопряжения") }',
    'placeholder = { Text("e.g. 123456") }': 'placeholder = { Text("например 123456") }',
    'pairing = true; status = "Pairing..."': 'pairing = true; status = "Сопряжение..."',
    'status = "Paired! Finding device…"': 'status = "Сопряжение выполнено. Поиск устройства…"',
    'status = "Paired! Connecting..."': 'status = "Сопряжение выполнено. Подключение..."',
    'pairing = false; status = "Getting device info..."': 'pairing = false; status = "Получение данных устройства..."',
    'Text(if (pairing) "Pairing..." else "Pair & Connect")': 'Text(if (pairing) "Сопряжение..." else "Сопрячь и подключить")',
    'title = { Text("USB Connection") }': 'title = { Text("USB OTG подключение") }',
    'Text("Scan USB Devices")': 'Text("Сканировать USB-устройства")',
    '"No ADB-capable USB devices found.\\n\\nMake sure:\\n- USB debugging is enabled\\n- Connected via OTG cable"': '"ADB-устройства по USB не найдены.\\n\\nПроверьте:\\n• включена USB-отладка\\n• устройство подключено через OTG"',
    'device.productName ?: "Unknown Device"': 'device.productName ?: "Неизвестное устройство"',
    'status = "Permission denied"': 'status = "Доступ запрещён"',
    'Text("Grant Permission")': 'Text("Разрешить доступ")',
    'device.productName ?: "USB Device"': 'device.productName ?: "USB-устройство"',
    'Text("Connect")': 'Text("Подключить")',
    'mutableStateListOf("--- Connected to $label ---")': 'mutableStateListOf("--- Подключено к $label ---")',
    'Text("Disconnect")': 'Text("Отключить")',
    'Text("Clear")': 'Text("Очистить")',
    'label = { Text("Command") }': 'label = { Text("Команда") }',
    'placeholder = { Text("e.g. ls /sdcard") }': 'placeholder = { Text("например ls /sdcard") }',
    'Text("Run")': 'Text("Выполнить")',
}

apply_replacements(MAIN, main_replacements)
apply_replacements(STREAM, common)

# Полностью заменяем старый блок «About», чтобы убрать старого разработчика
# и англоязычный дисклеймер из пользовательского интерфейса.
text = MAIN.read_text(encoding="utf-8")
about_pattern = re.compile(
    r"val annotated = buildAnnotatedString \{.*?\n            \}\n            AlertDialog\(",
    re.DOTALL,
)
about_replacement = '''val annotated = buildAnnotatedString {
                withStyle(SpanStyle(color = bodyColor)) {
                    append("STITCHLINK • ОРБИТА\\n\\n")
                    append("Система управления и трансляции Android-устройств.\\n\\n")
                    append("Разработчик:\\nSTITCHLINK Engineering\\n\\n")
                    append("Технический руководитель:\\nNord")
                }
            }
            AlertDialog('''
text, count = about_pattern.subn(about_replacement, text, count=1)
if count != 1:
    raise RuntimeError("Не удалось найти блок About для русификации")
MAIN.write_text(text, encoding="utf-8")

print("ORBITA patch applied")
