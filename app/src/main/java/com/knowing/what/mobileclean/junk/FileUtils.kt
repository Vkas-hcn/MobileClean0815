package com.knowing.what.mobileclean.junk

object FileUtils {
    fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 ->
                String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 ->
                String.format("%.1f MB", size / (1024.0 * 1024.0))
            else ->
                String.format("%.1f KB", size / 1024.0)
        }
    }
}
interface ScanProgressListener {
    fun onScanStarted()
    fun onScanProgress(progress: Int, currentPath: String)
    fun onFileFound(junkFile: JunkFile)
    fun onScanCompleted(totalFiles: Int, totalSize: Long)
    fun onScanError(error: Throwable)
}


interface CleanProgressListener {
    fun onCleanStarted()
    fun onCleanProgress(progress: Int, currentFile: String)
    fun onCleanCompleted(deletedCount: Int, deletedSize: Long)
    fun onCleanError(error: Throwable)
}