package com.knowing.what.mobileclean.big

sealed class FileType(val displayName: String, val minSize: Long) {
    object Image : FileType("Image", 1 * 1024L)
    object Video : FileType("Video", 10 * 1024L)
    object Audio : FileType("Audio", 1 * 1024L)
    object Docs : FileType("Docs", 1 * 1024L)
    object Download : FileType("Download", 1 * 1024L)
    object Zip : FileType("Zip", 1 * 1024L)
}

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val type: FileType,
    val dateAdded: Long,
    var isSelected: Boolean = false
) {
    val formattedSize: String by lazy {
        formatFileSize(size)
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.1f KB", size / 1024.0)
        }
    }
}

data class FileFilter(
    val type: String = "All types",
    val size: String = "All Size",
    val time: String = "All Time"
)

data class BigFileUiState(
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val filter: FileFilter = FileFilter(),
    val selectedCount: Int = 0,
    val error: String? = null
)

interface FileRepository {
    suspend fun scanAllFiles(): List<FileItem>
    suspend fun deleteFiles(files: List<FileItem>): DeleteResult
}

data class DeleteResult(
    val deletedCount: Int,
    val totalSize: Long,
    val success: Boolean
)