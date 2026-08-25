package tech.devline.scropy_ui

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tech.devline.scropy_ui.adb.AdbConnection
import tech.devline.scropy_ui.scrcpy.AudioPlayer
import tech.devline.scropy_ui.scrcpy.ControlSender
import tech.devline.scropy_ui.scrcpy.ScrcpySession
import tech.devline.scropy_ui.scrcpy.VideoDecoder
import tech.devline.scropy_ui.operator.ScrcpyStreamSettings
import tech.devline.scropy_ui.operator.StreamModePanel
import tech.devline.scropy_ui.operator.StreamConfig
import tech.devline.scropy_ui.ui.theme.ScropyTheme
import kotlin.math.roundToInt

class StreamActivity : ComponentActivity() {

    companion object {
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        const val EXTRA_USB_DEVICE = "usb_device"
        private const val DEFAULT_MAX_FPS = 30
        private const val DEFAULT_VIDEO_BIT_RATE = 4_000_000
    }

    private var adbConn: AdbConnection? = null
    private var session: ScrcpySession? = null
    private var videoDecoder: VideoDecoder? = null
    private var audioPlayer: AudioPlayer? = null
    private var controlSender: ControlSender? = null
    private var decodeJob: Job? = null
    private var audioJob: Job? = null
    private var pendingHost: String? = null
    private var pendingPort: Int = 5555
    private var pendingUsbDevice: UsbDevice? = null

    @Volatile private var surface: Surface? = null

    private var statusText = mutableStateOf("Подключение…")
    private var errorText = mutableStateOf<String?>(null)
    private var isConnected = mutableStateOf(false)
    private var screenOn = mutableStateOf(true)
    private var audioMuted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        val usbDevice: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_USB_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_USB_DEVICE)
        }

        val host = intent.getStringExtra(EXTRA_HOST)
        val port = intent.getIntExtra(EXTRA_PORT, 5555)
        if (usbDevice == null && host == null) { finish(); return }

        pendingHost = host
        pendingPort = port
        pendingUsbDevice = usbDevice

        setContent {

        var selectedMode by remember { mutableStateOf(StreamConfig.currentMode) }
        StreamModePanel(
            onModeChanged = { mode ->
                selectedMode = mode
                StreamConfig.currentMode = mode
            }
        )
            ScropyTheme {
                StreamScreen(
                    statusText = statusText.value,
                    errorText = errorText.value,
                    isConnected = isConnected.value,
                    screenOn = screenOn.value,
                    audioMuted = audioMuted.value,
                    onToggleScreen = {
                        val newOn = !screenOn.value
                        screenOn.value = newOn
                        controlSender?.sendDisplayPower(newOn)
                    },
                    onBack = { controlSender?.sendBackButton() },
                    onHome = { controlSender?.sendHomeButton() },
                    onRecent = { controlSender?.sendRecentAppsButton() },
                    onRotate = { controlSender?.sendRotateDevice() },
                    onVolumeUp = { controlSender?.sendVolumeUp() },
                    onVolumeDown = { controlSender?.sendVolumeDown() },
                    onMute = {
                        controlSender?.sendMute()
                        audioMuted.value = !audioMuted.value
                    },
                    onSurfaceReady = { s ->
                        surface = s
                        if (isConnected.value) {
                            videoDecoder?.restartCodec(s)
                            controlSender?.sendResetVideo()
                        } else if (usbDevice != null) {
                            connectUsbIfReady(usbDevice)
                        } else {
                            connectIfReady(host!!, port)
                        }
                    },
                    onSurfaceDestroyed = { surface = null },
                    onTouchEvent = { ev, w, h -> controlSender?.sendTouchEvent(ev, w, h) },
                    onScrollEvent = { ev, w, h -> controlSender?.sendScrollEvent(ev, w, h) },
                    onDisconnect = { disconnectAll(); finish() },
                    onRetry = { retryConnection() },
                )
            }
        }
    }

    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun connectIfReady(host: String, port: Int) {
        if (surface == null) return
        lifecycleScope.launch {
            try {
                statusText.value = "Подключение к $host:$port…"
                adbConn = AdbConnection.connectTcp(this@StreamActivity, host, port)
                statusText.value = "Запуск трансляции…"
                session = ScrcpySession.start(
                    context = this@StreamActivity,
                    adb = adbConn!!,
                    enableAudio = true,
                    maxFps = ScrcpyStreamSettings.fps,
                    videoBitRate = ScrcpyStreamSettings.bitrate,
                )
                startMediaSession(session!!, false)
            } catch (e: Exception) {
                android.util.Log.e("StreamActivity", "TCP connection failed", e)
                errorText.value = e.message ?: "Неизвестная ошибка"
                statusText.value = "Ошибка"
                isConnected.value = false
            }
        }
    }

    private fun connectUsbIfReady(device: UsbDevice) {
        if (surface == null) return
        lifecycleScope.launch {
            try {
                statusText.value = "Подключение по USB…"
                val usbManager = getSystemService(USB_SERVICE) as UsbManager
                adbConn = AdbConnection.connectUsb(this@StreamActivity, device, usbManager)
                statusText.value = "Запуск трансляции…"
                session = ScrcpySession.start(
                    context = this@StreamActivity,
                    adb = adbConn!!,
                    enableAudio = true,
                    maxFps = ScrcpyStreamSettings.fps,
                    videoBitRate = ScrcpyStreamSettings.bitrate,
                )
                startMediaSession(session!!, true)
            } catch (e: Exception) {
                android.util.Log.e("StreamActivity", "USB connection failed", e)
                errorText.value = e.message ?: "Ошибка USB-подключения"
                statusText.value = "Ошибка"
                isConnected.value = false
            }
        }
    }

    private fun startMediaSession(sess: ScrcpySession, usb: Boolean) {
        val info = sess.deviceInfo
        controlSender = ControlSender(sess.controlStream).also {
            it.deviceWidth = info.width
            it.deviceHeight = info.height
        }
        videoDecoder = VideoDecoder(
            stream = sess.videoStream,
            codecId = info.videoCodec,
            width = info.width,
            height = info.height,
            onResize = { w, h ->
                controlSender?.let { sender ->
                    sender.deviceWidth = w
                    sender.deviceHeight = h
                }
            },
        )
        statusText.value = "${info.name}${if (usb) " • USB" else ""}"
        isConnected.value = true
        errorText.value = null

        decodeJob = lifecycleScope.launch(Dispatchers.IO) {
            val s = surface ?: return@launch
            videoDecoder!!.start(s)
        }

        if (sess.audioStream != null && info.audioCodec != 0) {
            audioPlayer = AudioPlayer(sess.audioStream, info.audioCodec)
            audioJob = lifecycleScope.launch(Dispatchers.IO) { audioPlayer!!.start() }
        }
    }

    private fun disconnectAll() {
        decodeJob?.cancel()
        audioJob?.cancel()
        videoDecoder?.stop()
        audioPlayer?.stop()
        session?.close()
        adbConn?.close()
        adbConn = null
        session = null
        videoDecoder = null
        audioPlayer = null
        controlSender = null
    }

    private fun retryConnection() {
        if (surface == null) return
        errorText.value = null
        statusText.value = "Повторное подключение…"
        isConnected.value = false
        val u = pendingUsbDevice
        if (u != null) connectUsbIfReady(u) else pendingHost?.let { connectIfReady(it, pendingPort) }
    }

    override fun onPause() {
        super.onPause()
        videoDecoder?.releaseCodec()
        audioPlayer?.pauseAudio()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        audioPlayer?.resumeAudio()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectAll()
    }
}

@Composable
private fun StreamScreen(
    statusText: String,
    errorText: String?,
    isConnected: Boolean,
    screenOn: Boolean,
    audioMuted: Boolean,
    onToggleScreen: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecent: () -> Unit,
    onRotate: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onMute: () -> Unit,
    onSurfaceReady: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onTouchEvent: (MotionEvent, Int, Int) -> Unit,
    onScrollEvent: (MotionEvent, Int, Int) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) = onSurfaceReady(h.surface)
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) = Unit
                        override fun surfaceDestroyed(h: SurfaceHolder) = onSurfaceDestroyed()
                    })
                    setOnTouchListener { v, ev ->
                        if (ev.action == MotionEvent.ACTION_SCROLL) onScrollEvent(ev, v.width, v.height)
                        else onTouchEvent(ev, v.width, v.height)
                        true
                    }
                }
            },
        )

        if (!isConnected) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070A18)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("STITCHLINK", color = Color(0xFF76D9FF), style = MaterialTheme.typography.headlineMedium)
                    Text("ОРБИТА", color = Color(0xFFC27CFF), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(24.dp))
                    if (errorText != null) {
                        Text("Ошибка подключения", color = Color(0xFFFF6B8A), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Text(errorText, color = Color.White.copy(alpha = 0.75f))
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onRetry) { Text("Повторить") }
                            OutlinedButton(onClick = onDisconnect) { Text("Назад") }
                        }
                    } else {
                        CircularProgressIndicator(color = Color(0xFF8A7CFF))
                        Spacer(Modifier.height(16.dp))
                        Text(statusText, color = Color.White)
                    }
                }
            }
        }

        if (isConnected) {
            FloatingControls(
                screenOn = screenOn,
                audioMuted = audioMuted,
                onBack = onBack,
                onHome = onHome,
                onRecent = onRecent,
                onRotate = onRotate,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                onMute = onMute,
                onToggleScreen = onToggleScreen,
                onDisconnect = onDisconnect,
            )
        }
    }
}

@Composable
private fun BoxScope.FloatingControls(
    screenOn: Boolean,
    audioMuted: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecent: () -> Unit,
    onRotate: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onMute: () -> Unit,
    onToggleScreen: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }
    val puckPx = with(density) { 48.dp.toPx() }
    val marginPx = with(density) { 10.dp.toPx() }
    val gapPx = with(density) { 8.dp.toPx() }

    var offset by remember { mutableStateOf(Offset(-marginPx, marginPx)) }
    var expanded by remember { mutableStateOf(false) }

    fun clampOffset(o: Offset) = Offset(
        o.x.coerceIn(-(screenW - puckPx), 0f),
        o.y.coerceIn(0f, screenH - puckPx),
    )

    val puckCentreX = screenW - puckPx / 2f + offset.x
    val onLeft = puckCentreX < screenW / 2f

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    offset = clampOffset(offset + drag)
                }
            },
    ) {
        Surface(
            onClick = { expanded = !expanded },
            shape = CircleShape,
            color = Color(0xCC151B45),
            contentColor = Color(0xFF8FE7FF),
            shadowElevation = 10.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (expanded) "×" else "◈", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    if (expanded) {
        val menuY = offset.y + puckPx + gapPx
        val menuModifier = if (onLeft) {
            Modifier.align(Alignment.TopStart)
                .offset { IntOffset((screenW - puckPx + offset.x).roundToInt(), menuY.roundToInt()) }
        } else {
            Modifier.align(Alignment.TopEnd)
                .offset { IntOffset(offset.x.roundToInt(), menuY.roundToInt()) }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xE6121736),
            contentColor = Color.White,
            shadowElevation = 14.dp,
            modifier = menuModifier.widthIn(min = 210.dp),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    "STITCHLINK • ОРБИТА",
                    color = Color(0xFF9EEBFF),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
                Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuickButton("←", "Назад", onBack)
                    QuickButton("⌂", "Домой", onHome)
                    QuickButton("▤", "Недавние", onRecent)
                    QuickButton("↻", "Поворот", onRotate)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 6.dp))
                MenuItem("Громкость +", onVolumeUp)
                MenuItem("Громкость −", onVolumeDown)
                MenuItem(if (audioMuted) "Включить звук" else "Отключить звук", onMute)
                MenuItem(if (screenOn) "Выключить экран устройства" else "Включить экран устройства", onToggleScreen)
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 6.dp))
                MenuItem("Отключиться", onDisconnect, danger = true)
            }
        }
    }
}

@Composable
private fun RowScope.QuickButton(symbol: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF222A61),
        contentColor = Color.White,
        modifier = Modifier.weight(1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
        ) {
            Text(symbol, color = Color(0xFFB88CFF), style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit, danger: Boolean = false) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = if (danger) Color(0xFFFF7895) else Color.White,
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
