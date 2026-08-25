from pathlib import Path
import re

STREAM = Path("app/src/main/java/tech/devline/scropy_ui/StreamActivity.kt")
BUILD = Path("app/build.gradle.kts")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f"Не найден фрагмент v1.5.3: {label}")
    return text.replace(old, new, 1)


stream = STREAM.read_text(encoding="utf-8")

# Состояние автоматического восстановления соединения.
stream = replace_once(
    stream,
    '    private var pendingUsbDevice: UsbDevice? = null\n\n'
    '    @Volatile private var surface: Surface? = null\n',
    '    private var pendingUsbDevice: UsbDevice? = null\n'
    '    private var reconnectJob: Job? = null\n'
    '    private var reconnectAttempt: Int = 0\n'
    '    @Volatile private var intentionalDisconnect: Boolean = false\n\n'
    '    @Volatile private var surface: Surface? = null\n',
    "reconnect state",
)

# При ручном выходе не пытаемся поднимать соединение обратно.
stream = replace_once(
    stream,
    '                    onDisconnect = { disconnectAll(); finish() },\n',
    '                    onDisconnect = {\n'
    '                        intentionalDisconnect = true\n'
    '                        disconnectAll()\n'
    '                        finish()\n'
    '                    },\n',
    "manual disconnect",
)

# При смене качества сначала скрываем старую сессию, чтобы её остановка
# не была воспринята как аварийный обрыв.
stream = replace_once(
    stream,
    '                        if (changed) {\n'
    '                            disconnectAll()\n'
    '                            errorText.value = null\n'
    '                            statusText.value = "Применение качества: ${fps} FPS • ${bitRate / 1_000_000} Мбит/с…"\n'
    '                            isConnected.value = false\n',
    '                        if (changed) {\n'
    '                            isConnected.value = false\n'
    '                            reconnectJob?.cancel()\n'
    '                            reconnectJob = null\n'
    '                            reconnectAttempt = 0\n'
    '                            disconnectAll(cancelReconnect = false)\n'
    '                            errorText.value = null\n'
    '                            statusText.value = "Применение качества: ${fps} FPS • ${bitRate / 1_000_000} Мбит/с…"\n',
    "quality reconnect guard",
)

# Для Wi-Fi перед каждой попыткой заново спрашиваем живой ADB-порт через mDNS.
stream = replace_once(
    stream,
    '                statusText.value = "Подключение к $host:$port…"\n'
    '                adbConn = AdbConnection.connectTcp(this@StreamActivity, host, port)\n',
    '                statusText.value = "Подключение к $host…"\n'
    '                val livePort = resolveAdbPortViaMdns(this@StreamActivity, host, port, timeoutMs = 2200)\n'
    '                pendingPort = livePort\n'
    '                adbConn = AdbConnection.connectTcp(this@StreamActivity, host, livePort)\n',
    "refresh Wi-Fi ADB port",
)

# Ошибка первичного/повторного TCP-подключения запускает автоматическое восстановление.
stream = replace_once(
    stream,
    '            } catch (e: Exception) {\n'
    '                android.util.Log.e("StreamActivity", "TCP connection failed", e)\n'
    '                errorText.value = e.message ?: "Неизвестная ошибка"\n'
    '                statusText.value = "Ошибка"\n'
    '                isConnected.value = false\n'
    '            }\n',
    '            } catch (e: Exception) {\n'
    '                android.util.Log.e("StreamActivity", "TCP connection failed", e)\n'
    '                isConnected.value = false\n'
    '                if (!intentionalDisconnect) {\n'
    '                    scheduleAutoReconnect(e.message ?: "Wi‑Fi / ADB недоступен")\n'
    '                } else {\n'
    '                    errorText.value = e.message ?: "Неизвестная ошибка"\n'
    '                    statusText.value = "Ошибка"\n'
    '                }\n'
    '            }\n',
    "TCP auto reconnect",
)

# То же для USB.
stream = replace_once(
    stream,
    '            } catch (e: Exception) {\n'
    '                android.util.Log.e("StreamActivity", "USB connection failed", e)\n'
    '                errorText.value = e.message ?: "Ошибка USB-подключения"\n'
    '                statusText.value = "Ошибка"\n'
    '                isConnected.value = false\n'
    '            }\n',
    '            } catch (e: Exception) {\n'
    '                android.util.Log.e("StreamActivity", "USB connection failed", e)\n'
    '                isConnected.value = false\n'
    '                if (!intentionalDisconnect) {\n'
    '                    scheduleAutoReconnect(e.message ?: "USB-устройство недоступно")\n'
    '                } else {\n'
    '                    errorText.value = e.message ?: "Ошибка USB-подключения"\n'
    '                    statusText.value = "Ошибка"\n'
    '                }\n'
    '            }\n',
    "USB auto reconnect",
)

# Успешный старт сбрасывает счётчик попыток.
stream = replace_once(
    stream,
    '        statusText.value = "${info.name}${if (usb) " • USB" else ""}"\n'
    '        isConnected.value = true\n'
    '        errorText.value = null\n',
    '        reconnectAttempt = 0\n'
    '        reconnectJob = null\n'
    '        statusText.value = "${info.name}${if (usb) " • USB" else " • Wi‑Fi"}"\n'
    '        isConnected.value = true\n'
    '        errorText.value = null\n',
    "reset reconnect counter",
)

# Завершение видеопотока при активном экране считаем аварийным обрывом.
stream = replace_once(
    stream,
    '        decodeJob = lifecycleScope.launch(Dispatchers.IO) {\n'
    '            val s = surface ?: return@launch\n'
    '            videoDecoder!!.start(s)\n'
    '        }\n',
    '        decodeJob = lifecycleScope.launch(Dispatchers.IO) {\n'
    '            val s = surface ?: return@launch\n'
    '            try {\n'
    '                videoDecoder!!.start(s)\n'
    '            } catch (e: Exception) {\n'
    '                android.util.Log.w("StreamActivity", "Video stream stopped", e)\n'
    '            } finally {\n'
    '                if (\n'
    '                    !intentionalDisconnect &&\n'
    '                    isConnected.value &&\n'
    '                    lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)\n'
    '                ) {\n'
    '                    lifecycleScope.launch { scheduleAutoReconnect("видеопоток прерван") }\n'
    '                }\n'
    '            }\n'
    '        }\n',
    "video stream watcher",
)

# Разрешаем внутренней логике закрывать медиасессию, не отменяя таймер восстановления.
stream = replace_once(
    stream,
    '    private fun disconnectAll() {\n'
    '        decodeJob?.cancel()\n'
    '        audioJob?.cancel()\n'
    '        videoDecoder?.stop()\n'
    '        audioPlayer?.stop()\n'
    '        session?.close()\n'
    '        adbConn?.close()\n'
    '        adbConn = null\n'
    '        session = null\n'
    '        videoDecoder = null\n'
    '        audioPlayer = null\n'
    '        controlSender = null\n'
    '    }\n\n'
    '    private fun retryConnection() {\n',
    '    private fun disconnectAll(cancelReconnect: Boolean = true) {\n'
    '        if (cancelReconnect) {\n'
    '            reconnectJob?.cancel()\n'
    '            reconnectJob = null\n'
    '        }\n'
    '        decodeJob?.cancel()\n'
    '        audioJob?.cancel()\n'
    '        decodeJob = null\n'
    '        audioJob = null\n'
    '        videoDecoder?.stop()\n'
    '        audioPlayer?.stop()\n'
    '        session?.close()\n'
    '        adbConn?.close()\n'
    '        adbConn = null\n'
    '        session = null\n'
    '        videoDecoder = null\n'
    '        audioPlayer = null\n'
    '        controlSender = null\n'
    '    }\n\n'
    '    private fun currentUsbDevice(): UsbDevice? {\n'
    '        val original = pendingUsbDevice ?: return null\n'
    '        val manager = getSystemService(USB_SERVICE) as UsbManager\n'
    '        return manager.deviceList.values.firstOrNull { candidate ->\n'
    '            candidate.vendorId == original.vendorId &&\n'
    '                candidate.productId == original.productId\n'
    '        }\n'
    '    }\n\n'
    '    private fun scheduleAutoReconnect(reason: String) {\n'
    '        if (intentionalDisconnect || isFinishing || isDestroyed) return\n'
    '        if (reconnectJob?.isActive == true) return\n'
    '        if (reconnectAttempt >= 5) {\n'
    '            isConnected.value = false\n'
    '            statusText.value = "Связь потеряна"\n'
    '            errorText.value = "Автовосстановление не удалось после 5 попыток. Проверь USB / Wi‑Fi и нажми «Повторить»."\n'
    '            return\n'
    '        }\n'
    '        reconnectAttempt += 1\n'
    '        val attempt = reconnectAttempt\n'
    '        val delayMs = when (attempt) {\n'
    '            1 -> 1_000L\n'
    '            2 -> 2_000L\n'
    '            3 -> 4_000L\n'
    '            4 -> 6_000L\n'
    '            else -> 8_000L\n'
    '        }\n'
    '        isConnected.value = false\n'
    '        errorText.value = null\n'
    '        statusText.value = "Восстановление связи • $attempt/5 • ${delayMs / 1000}с\\n$reason"\n'
    '        reconnectJob = lifecycleScope.launch {\n'
    '            kotlinx.coroutines.delay(delayMs)\n'
    '            if (intentionalDisconnect || isFinishing || isDestroyed) return@launch\n'
    '            disconnectAll(cancelReconnect = false)\n'
    '            reconnectJob = null\n'
    '            val usb = pendingUsbDevice\n'
    '            if (usb != null) {\n'
    '                val current = currentUsbDevice()\n'
    '                if (current != null) {\n'
    '                    pendingUsbDevice = current\n'
    '                    connectUsbIfReady(current)\n'
    '                } else {\n'
    '                    scheduleAutoReconnect("ожидание USB-устройства")\n'
    '                }\n'
    '            } else {\n'
    '                pendingHost?.let { connectIfReady(it, pendingPort) }\n'
    '            }\n'
    '        }\n'
    '    }\n\n'
    '    private fun retryConnection() {\n',
    "autoreconnect engine",
)

# Ручной Retry обнуляет лимит и начинает чистую попытку.
stream = replace_once(
    stream,
    '    private fun retryConnection() {\n'
    '        if (surface == null) return\n'
    '        errorText.value = null\n'
    '        statusText.value = "Повторное подключение…"\n'
    '        isConnected.value = false\n'
    '        val u = pendingUsbDevice\n'
    '        if (u != null) connectUsbIfReady(u) else pendingHost?.let { connectIfReady(it, pendingPort) }\n'
    '    }\n',
    '    private fun retryConnection() {\n'
    '        if (surface == null) return\n'
    '        intentionalDisconnect = false\n'
    '        reconnectAttempt = 0\n'
    '        reconnectJob?.cancel()\n'
    '        reconnectJob = null\n'
    '        isConnected.value = false\n'
    '        disconnectAll(cancelReconnect = false)\n'
    '        errorText.value = null\n'
    '        statusText.value = "Повторное подключение…"\n'
    '        val u = pendingUsbDevice\n'
    '        if (u != null) {\n'
    '            val current = currentUsbDevice()\n'
    '            if (current != null) {\n'
    '                pendingUsbDevice = current\n'
    '                connectUsbIfReady(current)\n'
    '            } else {\n'
    '                scheduleAutoReconnect("ожидание USB-устройства")\n'
    '            }\n'
    '        } else {\n'
    '            pendingHost?.let { connectIfReady(it, pendingPort) }\n'
    '        }\n'
    '    }\n',
    "manual retry reset",
)

# Уничтожение Activity всегда является намеренным завершением.
stream = replace_once(
    stream,
    '    override fun onDestroy() {\n'
    '        super.onDestroy()\n'
    '        disconnectAll()\n'
    '    }\n',
    '    override fun onDestroy() {\n'
    '        intentionalDisconnect = true\n'
    '        disconnectAll()\n'
    '        super.onDestroy()\n'
    '    }\n',
    "destroy guard",
)

STREAM.write_text(stream, encoding="utf-8")

# Версия v1.5.3.
build = BUILD.read_text(encoding="utf-8")
build, version_code_count = re.subn(r'versionCode\s*=\s*\d+', 'versionCode = 7', build, count=1)
build, version_name_count = re.subn(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.5.3"', build, count=1)
if version_code_count != 1 or version_name_count != 1:
    raise RuntimeError("Не удалось обновить версию для v1.5.3")
build = build.replace("STITCHLINK • ОРБИТА v1.5.2", "STITCHLINK • ОРБИТА v1.5.3")
BUILD.write_text(build, encoding="utf-8")

print("ORBITA v1.5.3 auto reconnect applied")
