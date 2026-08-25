package tech.devline.scropy_ui.operator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamModePanel(
    onModeChanged: (StreamConfig.Mode) -> Unit,
) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("STITCHLINK STREAM")

        Button(onClick = {
            StreamConfig.setMode(StreamConfig.Mode.ECONOMY)
            onModeChanged(StreamConfig.Mode.ECONOMY)
        }) {
            Text("⚡ ECONOMY  30 FPS / 4 Mbps")
        }

        Button(onClick = {
            StreamConfig.setMode(StreamConfig.Mode.FLIGHT)
            onModeChanged(StreamConfig.Mode.FLIGHT)
        }) {
            Text("🚁 FLIGHT  60 FPS / 12 Mbps")
        }

        Button(onClick = {
            StreamConfig.setMode(StreamConfig.Mode.MAXIMUM)
            onModeChanged(StreamConfig.Mode.MAXIMUM)
        }) {
            Text("🔥 MAXIMUM  60 FPS / 20 Mbps")
        }
    }
}
