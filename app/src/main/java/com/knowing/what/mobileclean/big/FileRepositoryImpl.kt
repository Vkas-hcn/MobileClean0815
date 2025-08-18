package com.knowing.what.mobileclean.big

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepositoryImpl(private val context: Context) : FileRepository {

    override suspend fun scanAllFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<FileItem>()

        try {
            scanMediaFiles(allFiles)
            scanDocumentFiles(allFiles)
            scanDownloadFiles(allFiles)
            scanZipFiles(allFiles)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        allFiles
    }

    private suspend fun scanMediaFiles(allFiles: MutableList<FileItem>) {
        scanMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, FileType.Image, allFiles)
        scanMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, FileType.Video, allFiles)
        scanMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, FileType.Audio, allFiles)
    }

    private fun scanMediaStore(uri: Uri, type: FileType, allFiles: MutableList<FileItem>) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    val path = cursor.getString(pathColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000

                    if (size > type.minSize) {
                        val file = File(path)
                        if (file.exists()) {
                            allFiles.add(FileItem(name, path, size, type, dateAdded))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanDocumentFiles(allFiles: MutableList<FileItem>) {
        val documentExtensions = arrayOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
        scanFilesByExtensions(documentExtensions, FileType.Docs, allFiles)
    }

    private fun scanDownloadFiles(allFiles: MutableList<FileItem>) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        scanDirectory(downloadDir, FileType.Download, allFiles)
    }

    private fun scanZipFiles(allFiles: MutableList<FileItem>) {
        val zipExtensions = arrayOf("zip", "rar", "7z", "tar", "gz")
        scanFilesByExtensions(zipExtensions, FileType.Zip, allFiles)
    }

    private fun scanFilesByExtensions(extensions: Array<String>, type: FileType, allFiles: MutableList<FileItem>) {
        val externalStorage = Environment.getExternalStorageDirectory()
        scanDirectoryForExtensions(externalStorage, extensions, type, allFiles)
    }

    private fun scanDirectoryForExtensions(dir: File, extensions: Array<String>, type: FileType, allFiles: MutableList<FileItem>) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    if (!file.name.startsWith(".") && !file.name.equals("Android", ignoreCase = true)) {
                        scanDirectoryForExtensions(file, extensions, type, allFiles)
                    }
                } else if (file.isFile) {
                    val extension = file.extension.lowercase()
                    if (extensions.contains(extension) && file.length() > type.minSize) {
                        allFiles.add(FileItem(
                            file.name,
                            file.absolutePath,
                            file.length(),
                            type,
                            file.lastModified()
                        ))
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun scanDirectory(dir: File, type: FileType, allFiles: MutableList<FileItem>) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() > type.minSize) {
                    allFiles.add(FileItem(
                        file.name,
                        file.absolutePath,
                        file.length(),
                        type,
                        file.lastModified()
                    ))
                }
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun deleteFiles(files: List<FileItem>): DeleteResult = withContext(Dispatchers.IO) {
        var totalDeletedSize = 0L
        var deletedCount = 0

        files.forEach { fileItem ->
            try {
                val file = File(fileItem.path)
                if (file.exists() && deleteFileOrDirectory(file)) {
                    totalDeletedSize += fileItem.size
                    deletedCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        DeleteResult(deletedCount, totalDeletedSize, deletedCount > 0)
    }

    private fun deleteFileOrDirectory(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteFileOrDirectory(child)
            }
        }
        return file.delete()
    }
}