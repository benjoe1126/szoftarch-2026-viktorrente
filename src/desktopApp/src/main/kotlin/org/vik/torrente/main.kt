package org.vik.torrente

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.awt.Desktop
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 680.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Viktorrente",
        state = windowState
    ) {
        window.minimumSize = Dimension(720, 480)
        App(
            defaultDownloadDirectory = getDefaultDownloadDirectory(),
            onOpenDirectory = { openDirectory(it) }
        )
    }
}

private fun openDirectory(directoryPath: String) {
    try {
        val dir = File(directoryPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(dir)
        } else {
            println("Desktop open not supported for path: $directoryPath")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getDefaultDownloadDirectory(): String {
    val userHome = System.getProperty("user.home") ?: "."
    val downloads = File(userHome, "Downloads")
    return if (downloads.exists()) downloads.absolutePath else userHome
}
