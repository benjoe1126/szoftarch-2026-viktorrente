package org.vik.torrente

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 680.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Viktorrente",
        state = windowState
    ) {
        window.minimumSize = Dimension(720, 480)
        App()
    }
}
