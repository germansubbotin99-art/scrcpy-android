from pathlib import Path
import re

STREAM = Path("app/src/main/java/tech/devline/scropy_ui/StreamActivity.kt")
BUILD = Path("app/build.gradle.kts")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Не найден фрагмент v1.5.1: {label}")
    return text.replace(old, new, 1)


stream = STREAM.read_text(encoding="utf-8")

# Быстрые операторские профили внутри уже существующего меню качества.
stream = replace_once(
    stream,
    '                if (showQuality) {\n'
    '                    MenuItem("FPS: $selectedFps  • нажми для смены", {\n',
    '                if (showQuality) {\n'
    '                    MenuItem("⚡ Экономичный • 30 FPS • 2 Мбит/с", {\n'
    '                        showQuality = false\n'
    '                        selectedFps = 30\n'
    '                        selectedBitRate = 2_000_000\n'
    '                        onApplyStreamSettings(30, 2_000_000)\n'
    '                    })\n'
    '                    MenuItem("✈ Полёт • 60 FPS • 8 Мбит/с", {\n'
    '                        showQuality = false\n'
    '                        selectedFps = 60\n'
    '                        selectedBitRate = 8_000_000\n'
    '                        onApplyStreamSettings(60, 8_000_000)\n'
    '                    })\n'
    '                    MenuItem("◆ Максимум • 60 FPS • 12 Мбит/с", {\n'
    '                        showQuality = false\n'
    '                        selectedFps = 60\n'
    '                        selectedBitRate = 12_000_000\n'
    '                        onApplyStreamSettings(60, 12_000_000)\n'
    '                    })\n'
    '                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f), modifier = Modifier.padding(vertical = 4.dp))\n'
    '                    MenuItem("FPS: $selectedFps  • нажми для смены", {\n',
    "profile menu",
)

# HUD показывает, какой профиль фактически активен.
stream = replace_once(
    stream,
    'private fun BoxScope.OrbitaHud(status: String, fps: Int, bitRate: Int) {\n'
    '    Surface(\n',
    'private fun BoxScope.OrbitaHud(status: String, fps: Int, bitRate: Int) {\n'
    '    val profile = when {\n'
    '        fps == 30 && bitRate == 2_000_000 -> "ЭКОНОМИЧНЫЙ"\n'
    '        fps == 60 && bitRate == 8_000_000 -> "ПОЛЁТ"\n'
    '        fps == 60 && bitRate == 12_000_000 -> "МАКСИМУМ"\n'
    '        else -> "РУЧНОЙ"\n'
    '    }\n'
    '    Surface(\n',
    "HUD profile label",
)

stream = replace_once(
    stream,
    '            "$fps FPS • ${bitRate / 1_000_000} Мбит/с • LOW LOAD",\n',
    '            "$profile • $fps FPS • ${bitRate / 1_000_000} Мбит/с",\n',
    "HUD profile text",
)

STREAM.write_text(stream, encoding="utf-8")

# Отдельная версия сборки, чтобы её было легко отличить при тестировании.
build = BUILD.read_text(encoding="utf-8")
build, version_code_count = re.subn(r'versionCode\s*=\s*\d+', 'versionCode = 5', build, count=1)
build, version_name_count = re.subn(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.5.1"', build, count=1)
if version_code_count != 1 or version_name_count != 1:
    raise RuntimeError("Не удалось обновить версию для v1.5.1")
build = build.replace("STITCHLINK • ОРБИТА v1.5", "STITCHLINK • ОРБИТА v1.5.1")
BUILD.write_text(build, encoding="utf-8")

print("ORBITA v1.5.1 operator profiles applied")
