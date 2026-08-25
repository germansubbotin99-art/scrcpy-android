package tech.devline.scropy_ui.operator

object StreamConfig {

    enum class Mode {
        ECONOMY,
        FLIGHT,
        PILOT,
        MAXIMUM
    }

    data class Profile(
        val fps: Int,
        val bitrate: Int
    )

    @Volatile
    var currentMode: Mode = Mode.PILOT

    fun current(): Profile {
        return when (currentMode) {
            Mode.ECONOMY -> Profile(
                fps = 30,
                bitrate = 4_000_000
            )

            Mode.FLIGHT -> Profile(
                fps = 60,
                bitrate = 12_000_000
            )

            Mode.PILOT -> Profile(
                fps = 60,
                bitrate = 20_000_000
            )

            Mode.MAXIMUM -> Profile(
                fps = 60,
                bitrate = 30_000_000
            )
        }
    }
}
