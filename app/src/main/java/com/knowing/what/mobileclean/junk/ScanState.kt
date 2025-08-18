package com.knowing.what.mobileclean.junk

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val progress: Int, val currentPath: String) : ScanState()
    data class Completed(val totalFiles: Int, val totalSize: Long) : ScanState()
    data class Error(val error: Throwable) : ScanState()
}


sealed class CleanState {
    object Idle : CleanState()
    data class Cleaning(val progress: Int, val currentFile: String) : CleanState()
    data class Completed(val deletedCount: Int, val deletedSize: Long) : CleanState()
    data class Error(val error: Throwable) : CleanState()
}