package tech.devline.scropy_ui.operator

enum class DeviceType {
    DJI,
    AUTEL,
    FPV,
    CUSTOM
}

data class StreamProfile(
    val name: String,
    val fps: Int,
    val bitrateMbps: Int
)

data class DeviceProfile(
    val deviceName: String,
    val type: DeviceType,
    val streamProfile: StreamProfile
)

object OrbitaProfiles {
    val flight = StreamProfile("FLIGHT", 60, 12)
    val economy = StreamProfile("ECONOMY", 30, 4)
    val maximum = StreamProfile("MAXIMUM", 60, 20)
}
