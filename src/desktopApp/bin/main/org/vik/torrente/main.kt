package org.vik.torrente

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.vik.torrente.engine.decode

fun main() = application {

    decode("Hello World!")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Viktorrente",
    ) {
        App()
    }
}