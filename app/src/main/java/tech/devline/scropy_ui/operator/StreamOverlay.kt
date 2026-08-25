package tech.devline.scropy_ui.operator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StreamOverlay(
    modifier: Modifier = Modifier
) {
    val profile = StreamConfig.current()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC12172F),
        contentColor = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text("STITCHLINK ORBITA")
            Text("STREAM: ${profile.name}")
            Text("${profile.fps} FPS")
            Text("${profile.bitrate / 1_000_000} Mbps")
        }
    }
}
