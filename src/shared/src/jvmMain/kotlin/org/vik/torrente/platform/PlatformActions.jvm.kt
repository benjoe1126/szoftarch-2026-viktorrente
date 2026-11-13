package org.vik.torrente.platform

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual object PlatformActions {

    actual fun chooseTorrentFile(onResult: (filePath: String, fileName: String) -> Unit) {
        SwingUtilities.invokeLater {
            try {
                val dialog = FileDialog(null as Frame?, "Select .torrent file", FileDialog.LOAD).apply {
                    filenameFilter = FilenameFilter { _, name ->
                        name.endsWith(".torrent", ignoreCase = true)
                    }
                    isVisible = true
                }

                val dir = dialog.directory
                val file = dialog.file
                if (!dir.isNullOrBlank() && !file.isNullOrBlank()) {
                    val fullFile = File(dir, file)
                    onResult(fullFile.absolutePath, fullFile.name)
                }
            } catch (e: Exception) {
                // Fallback to JFileChooser if FileDialog encounters an issue
                try {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Select .torrent file"
                        fileFilter = object : javax.swing.filechooser.FileFilter() {
                            override fun accept(f: File): Boolean {
                                return f.isDirectory || f.name.endsWith(".torrent", ignoreCase = true)
                            }
                            override fun getDescription(): String = "Torrent Files (*.torrent)"
                        }
                    }
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                        val selected = chooser.selectedFile
                        onResult(selected.absolutePath, selected.name)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    actual fun chooseDirectory(initialPath: String?, onResult: (directoryPath: String) -> Unit) {
        SwingUtilities.invokeLater {
            try {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select Download Destination Folder"
                    if (!initialPath.isNullOrBlank()) {
                        val initialFile = File(initialPath)
                        if (initialFile.exists()) {
                            currentDirectory = initialFile
                        }
                    }
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION && chooser.selectedFile != null) {
                    onResult(chooser.selectedFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun openDirectory(directoryPath: String) {
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

    actual fun getDefaultDownloadDirectory(): String {
        val userHome = System.getProperty("user.home") ?: "."
        val downloads = File(userHome, "Downloads")
        return if (downloads.exists()) downloads.absolutePath else userHome
    }
}
