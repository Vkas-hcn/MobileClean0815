package com.knowing.what.mobileclean.junk

import com.knowing.what.mobileclean.R

enum class JunkFileTypeMy(val displayName: String, val iconRes: Int) {
    APP_CACHE("App Cache", R.drawable.ic_cache),
    APK_FILES("Apk Files", R.drawable.ic_apk),
    LOG_FILES("Log Files", R.drawable.ic_xitongrizhi),
    TEMP_FILES("Temp Files", R.drawable.ic_temp),
    OTHER("Other", R.drawable.ic_ad)
}


data class JunkFile(
    val name: String,
    val path: String,
    val size: Long,
    val type: JunkFileTypeMy,
    var isSelected: Boolean = false
) {
    val formattedSize: String by lazy {
        FileUtils.formatFileSize(size)
    }
}

data class JunkCategory(
    val type: JunkFileTypeMy,
    val files: MutableList<JunkFile> = mutableListOf(),
    var isExpanded: Boolean = false,
    var isSelected: Boolean = false
) {
    val name: String get() = type.displayName
    val iconRes: Int get() = type.iconRes

    val totalSize: Long
        get() = files.sumOf { it.size }

    val selectedSize: Long
        get() = files.filter { it.isSelected }.sumOf { it.size }

    val formattedTotalSize: String
        get() = FileUtils.formatFileSize(totalSize)

    val hasSelectedFiles: Boolean
        get() = files.any { it.isSelected }
}