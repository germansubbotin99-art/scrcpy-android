package tech.devline.scropy_ui.operator

/**
 * Runtime state for operator HUD.
 * Keeps active stream profile information ready for UI binding.
 */
object OperatorStreamState {
    var activeProfile: StreamProfileManager.Profile = StreamProfileManager.Profile.FLIGHT

    fun hudText(): String {
        val settings = StreamProfileManager.get(activeProfile)
        return "PROFILE: ${settings.label}\n${settings.fps} FPS\n${settings.bitrate / 1_000_000} Mbps"
    }
}
