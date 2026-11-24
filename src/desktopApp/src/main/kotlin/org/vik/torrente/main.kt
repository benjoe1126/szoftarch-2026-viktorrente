package org.vik.torrente

import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.database.JvmDatabaseDriverFactory
import java.awt.Dimension
import java.awt.Desktop
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 680.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Viktorrente",
        icon = painterResource("icon.png"),
        state = windowState
    ) {
        window.minimumSize = Dimension(720, 480)
        val repository = remember { TorrentRepository(JvmDatabaseDriverFactory()) }
        App(
            defaultDownloadDirectory = getDefaultDownloadDirectory(),
            onOpenDirectory = { openDirectory(it) },
            repository = repository,
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
