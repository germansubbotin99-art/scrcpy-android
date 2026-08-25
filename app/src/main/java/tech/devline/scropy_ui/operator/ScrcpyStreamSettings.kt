package tech.devline.scropy_ui.operator

object ScrcpyStreamSettings {

    val fps: Int
        get() = StreamConfig.current().fps

    val bitrate: Int
        get() = StreamConfig.current().bitrate
}
