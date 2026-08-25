package tech.devline.scropy_ui.operator

/**
 * STITCHLINK ORBITA stream profile manager.
 * Central place for video profiles used by operator mode.
 */
object StreamProfileManager {
    enum class Profile {
        ECONOMY,
        FLIGHT,
        MAXIMUM
    }

    data class StreamSettings(
        val fps: Int,
        val bitrate: Int,
        val label: String
    )

    fun get(profile: Profile): StreamSettings = when (profile) {
        Profile.ECONOMY -> StreamSettings(
            fps = 30,
            bitrate = 4_000_000,
            label = "ECONOMY"
        )

        Profile.FLIGHT -> StreamSettings(
            fps = 60,
            bitrate = 12_000_000,
            label = "FLIGHT"
        )

        Profile.MAXIMUM -> StreamSettings(
            fps = 60,
            bitrate = 20_000_000,
            label = "MAXIMUM"
        )
    }
}
