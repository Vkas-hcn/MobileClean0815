package com.knowing.what.mobileclean.big

class ScanFilesUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(): List<FileItem> {
        return repository.scanAllFiles()
    }
}

class DeleteFilesUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(files: List<FileItem>): DeleteResult {
        return repository.deleteFiles(files)
    }
}

class FilterFilesUseCase {
    operator fun invoke(
        files: List<FileItem>,
        filter: FileFilter
    ): List<FileItem> {
        return files.filter { file ->
            matchesTypeFilter(file, filter.type) &&
                    matchesSizeFilter(file, filter.size) &&
                    matchesTimeFilter(file, filter.time)
        }
    }

    private fun matchesTypeFilter(file: FileItem, typeFilter: String): Boolean {
        return when (typeFilter) {
            "All types" -> true
            "Image" -> file.type is FileType.Image
            "Video" -> file.type is FileType.Video
            "Audio" -> file.type is FileType.Audio
            "Docs" -> file.type is FileType.Docs
            "Download" -> file.type is FileType.Download
            "Zip" -> file.type is FileType.Zip
            else -> true
        }
    }

    private fun matchesSizeFilter(file: FileItem, sizeFilter: String): Boolean {
        val sizeLimit = when (sizeFilter) {
            "All Size" -> 0L
            ">1MB" -> 1 * 1024 * 1024L
            ">5MB" -> 5 * 1024 * 1024L
            ">10MB" -> 10 * 1024 * 1024L
            ">20MB" -> 20 * 1024 * 1024L
            ">50MB" -> 50 * 1024 * 1024L
            ">100MB" -> 100 * 1024 * 1024L
            ">200MB" -> 200 * 1024 * 1024L
            ">500MB" -> 500 * 1024 * 1024L
            else -> 0L
        }
        return file.size > sizeLimit
    }

    private fun matchesTimeFilter(file: FileItem, timeFilter: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeLimit = when (timeFilter) {
            "All Time" -> 0L
            "Within 1 day" -> currentTime - 24 * 60 * 60 * 1000L
            "Within 1 week" -> currentTime - 7 * 24 * 60 * 60 * 1000L
            "Within 1 month" -> currentTime - 30 * 24 * 60 * 60 * 1000L
            "Within 3 month" -> currentTime - 90 * 24 * 60 * 60 * 1000L
            "Within 6 month" -> currentTime - 180 * 24 * 60 * 60 * 1000L
            else -> 0L
        }
        return file.dateAdded >= timeLimit
    }
}