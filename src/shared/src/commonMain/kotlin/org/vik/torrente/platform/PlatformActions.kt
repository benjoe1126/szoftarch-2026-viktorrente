package org.vik.torrente.platform

/**
 * Platform-specific operations for desktop file dialogues and OS integrations.
 */
expect object PlatformActions {
    /**
     * Opens a file picker dialog allowing the user to select a .torrent file.
     */
    fun chooseTorrentFile(onResult: (filePath: String, fileName: String) -> Unit)

    /**
     * Opens a directory picker dialog allowing the user to select a download destination folder.
     */
    fun chooseDirectory(initialPath: String?, onResult: (directoryPath: String) -> Unit)

    /**
     * Opens the given directory path in the operating system's native file explorer.
     */
    fun openDirectory(directoryPath: String)

    /**
     * Returns the default download folder path on the host system (e.g. ~/Downloads).
     */
    fun getDefaultDownloadDirectory(): String
}
