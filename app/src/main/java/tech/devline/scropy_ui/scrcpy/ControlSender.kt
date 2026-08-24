package tech.devline.scropy_ui.scrcpy

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import tech.devline.scropy_ui.adb.AdbStream
import java.io.IOException

private const val TAG = "ControlSender"

class ControlSender(private val stream: AdbStream) {
    @Volatile var deviceWidth: Int = 0
    @Volatile var deviceHeight: Int = 0

    fun sendTouchEvent(event: MotionEvent, surfaceWidth: Int, surfaceHeight: Int) {
        if (deviceWidth == 0 || deviceHeight == 0) return
        val scrcpyAct = mapAction(event.actionMasked) ?: return
        val pointerIdx = event.actionIndex
        val pointerId = normalizePointerId(event.getPointerId(pointerIdx))
        val x = (event.getX(pointerIdx) / surfaceWidth * deviceWidth).toInt()
        val y = (event.getY(pointerIdx) / surfaceHeight * deviceHeight).toInt()
        send(ScrcpyProtocol.buildTouchEvent(scrcpyAct, pointerId, x, y, deviceWidth, deviceHeight, event.getPressure(pointerIdx), event.actionButton, event.buttonState))
    }

    fun sendScrollEvent(event: MotionEvent, surfaceWidth: Int, surfaceHeight: Int) {
        val x = (event.x / surfaceWidth * deviceWidth).toInt()
        val y = (event.y / surfaceHeight * deviceHeight).toInt()
        send(ScrcpyProtocol.buildScrollEvent(x, y, deviceWidth, deviceHeight, event.getAxisValue(MotionEvent.AXIS_HSCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL)))
    }

    fun sendKeyEvent(action: Int, keycode: Int, repeat: Int = 0, metaState: Int = 0) = send(ScrcpyProtocol.buildKeyEvent(action, keycode, repeat, metaState))
    fun sendText(text: String) = send(ScrcpyProtocol.buildTextEvent(text))

    private fun pressKey(keycode: Int) {
        sendKeyEvent(KeyEvent.ACTION_DOWN, keycode)
        sendKeyEvent(KeyEvent.ACTION_UP, keycode)
    }

    fun sendBackButton() = send(ScrcpyProtocol.buildEmpty(ScrcpyProtocol.TYPE_BACK_OR_SCREEN_ON))
    fun sendHomeButton() = pressKey(KeyEvent.KEYCODE_HOME)
    fun sendRecentAppsButton() = pressKey(KeyEvent.KEYCODE_APP_SWITCH)
    fun sendVolumeUp() = pressKey(KeyEvent.KEYCODE_VOLUME_UP)
    fun sendVolumeDown() = pressKey(KeyEvent.KEYCODE_VOLUME_DOWN)
    fun sendMute() = pressKey(KeyEvent.KEYCODE_VOLUME_MUTE)
    fun sendCollapseNotifications() = send(ScrcpyProtocol.buildEmpty(ScrcpyProtocol.TYPE_COLLAPSE_PANELS))
    fun sendExpandNotifications() = send(ScrcpyProtocol.buildEmpty(ScrcpyProtocol.TYPE_EXPAND_NOTIFICATION))
    fun sendRotateDevice() = send(ScrcpyProtocol.buildEmpty(ScrcpyProtocol.TYPE_ROTATE_DEVICE))
    fun sendResetVideo() = send(ScrcpyProtocol.buildEmpty(ScrcpyProtocol.TYPE_RESET_VIDEO))
    fun sendDisplayPower(on: Boolean) = send(ScrcpyProtocol.buildDisplayPower(on))

    private fun send(data: ByteArray) {
        try { stream.write(data) } catch (e: IOException) { Log.w(TAG, "Control send failed: ${e.message}") }
    }

    private fun mapAction(action: Int): Int? = when (action) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> 1
        MotionEvent.ACTION_MOVE -> 2
        else -> null
    }

    private fun normalizePointerId(androidId: Int): Long = if (androidId == 0) ScrcpyProtocol.POINTER_MOUSE else androidId.toLong()
}
