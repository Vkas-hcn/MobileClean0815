package com.knowing.what.mobileclean.junk

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class JunkFileScanner {

    fun categorizeFile(file: File): JunkFile? {
        val fileName = file.name.lowercase()
        val filePath = file.absolutePath.lowercase()
        val fileSize = file.length()

        if (fileSize < 100) return null

        val type = when {
            filePath.contains("/cache/") ||
                    fileName.endsWith(".cache") ||
                    fileName.contains("cache") ||
                    filePath.contains("/app_cache/") ||
                    filePath.contains("/webview/") ||
                    fileName.endsWith(".dex") && filePath.contains("cache") ->
                JunkFileTypeMy.APP_CACHE

            fileName.endsWith(".apk") ||
                    fileName.endsWith(".xapk") ||
                    fileName.endsWith(".apks") ->
                JunkFileTypeMy.APK_FILES

            fileName.endsWith(".log") ||
                    fileName.endsWith(".txt") && (filePath.contains("log") || fileName.contains("log")) ||
                    fileName.endsWith(".crash") ||
                    fileName.startsWith("log") ||
                    filePath.contains("/logs/") ->
                JunkFileTypeMy.LOG_FILES

            fileName.endsWith(".tmp") ||
                    fileName.endsWith(".temp") ||
                    filePath.contains("/temp/") ||
                    filePath.contains("/.temp") ||
                    fileName.startsWith("tmp") ||
                    fileName.startsWith("temp") ||
                    filePath.contains("/temporary/") ||
                    filePath.contains("/.thumbnails/") ->
                JunkFileTypeMy.TEMP_FILES

            fileName.endsWith(".bak") ||
                    fileName.endsWith(".old") ||
                    fileName.startsWith("~") ||
                    fileName.contains("backup") ||
                    fileName.endsWith(".swp") ||
                    fileName.endsWith(".swo") ||
                    fileName.startsWith(".") && fileName.length > 10 ||
                    filePath.contains("/trash/") ||
                    filePath.contains("/recycle/") ->
                JunkFileTypeMy.OTHER

            fileSize > 10 * 1024 * 1024 && filePath.contains("/download") ->
                JunkFileTypeMy.OTHER

            else -> null
        }

        return if (type != null) {
            JunkFile(file.name, file.absolutePath, fileSize, type)
        } else null
    }
}


class JunkFileRepository(
    private val context: Context,
    private val scanner: JunkFileScanner = JunkFileScanner()
) {

    suspend fun scanJunkFiles(listener: ScanProgressListener) = withContext(Dispatchers.IO) {
        listener.onScanStarted()

        val rootDirs = getScanDirectories()
        var progress = 0
        val totalDirs = rootDirs.size
        var totalSize = 0L
        var totalFiles = 0

        try {
            for (rootDir in rootDirs) {
                withContext(Dispatchers.Main) {
                    listener.onScanProgress(progress * 100 / totalDirs, rootDir.absolutePath)
                }

                scanDirectory(rootDir, listener, { size ->
                    totalSize += size
                    totalFiles++
                    if (totalSize > 500 * 1024 * 1024) { // 超过500MB停止
                        return@scanDirectory false
                    }
                    true
                })

                progress++
                delay(200)
            }

            withContext(Dispatchers.Main) {
                listener.onScanCompleted(totalFiles, totalSize)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                listener.onScanError(e)
            }
        }
    }

    suspend fun cleanFiles(files: List<JunkFile>, listener: CleanProgressListener) =
        withContext(Dispatchers.IO) {
            listener.onCleanStarted()

            var deletedCount = 0
            var deletedSize = 0L

            try {
                files.forEachIndexed { index, junkFile ->
                    withContext(Dispatchers.Main) {
                        listener.onCleanProgress(
                            (index * 100) / files.size,
                            junkFile.name
                        )
                    }

                    val file = File(junkFile.path)
                    if (!file.exists() || file.delete()) {
                        deletedCount++
                        deletedSize += junkFile.size
                    }

                    delay(50)
                }

                withContext(Dispatchers.Main) {
                    listener.onCleanCompleted(deletedCount, deletedSize)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onCleanError(e)
                }
            }
        }

    private fun getScanDirectories(): List<File> {
        val rootDirs = mutableListOf<File>()

        Environment.getExternalStorageDirectory()?.let { rootDirs.add(it) }
        context.externalCacheDir?.let { rootDirs.add(it) }
        context.cacheDir?.let { rootDirs.add(it) }

        val commonTrashDirs = arrayOf(
            "/storage/emulated/0/Android/data",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Pictures/.thumbnails",
            "/storage/emulated/0/DCIM/.thumbnails",
            "/storage/emulated/0/.android_secure",
            "/storage/emulated/0/Documents"
        )

        commonTrashDirs.forEach { path ->
            val dir = File(path)
            if (dir.exists() && dir.canRead()) {
                rootDirs.add(dir)
            }
        }

        return rootDirs
    }

    private suspend fun scanDirectory(
        dir: File,
        listener: ScanProgressListener,
        onFileFound: (Long) -> Boolean,
        depth: Int = 0
    ) {
        if (depth > 4) return

        try {
            val files = dir.listFiles() ?: return

            for (file in files) {
                when {
                    file.isDirectory -> {
                        val skipDirs = arrayOf("proc", "sys", "dev", "system", "root")
                        if (!skipDirs.any { file.name.contains(it, true) }) {
                            scanDirectory(file, listener, onFileFound, depth + 1)
                        }
                    }
                    file.isFile -> {
                        val junkFile = scanner.categorizeFile(file)
                        if (junkFile != null) {
                            withContext(Dispatchers.Main) {
                                listener.onFileFound(junkFile)
                            }
                            if (!onFileFound(junkFile.size)) {
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // 忽略无权限访问的目录
        } catch (e: Exception) {
            // 忽略其他异常
        }
    }
}