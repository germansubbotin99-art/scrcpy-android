package tech.devline.scropy_ui.operator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * STITCHLINK ORBITA operator overlay foundation.
 * HUD elements will be connected to live stream metrics in the next step.
 */
@Composable
fun OperatorHud(
    connection: String,
    transport: String,
    fps: Int,
    bitrateMbps: Int,
    profile: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text("STITCHLINK • ОРБИТА")
        Text(connection)
        Text(transport)
        Text("$fps FPS • ${bitrateMbps} Mbps")
        Text("Режим: $profile")
    }
}
