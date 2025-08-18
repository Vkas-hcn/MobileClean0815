package com.knowing.what.mobileclean.main

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.DecimalFormat
import kotlin.math.max

data class StorageInfo(
    val freeStorage: String,
    val usedStorage: String,
    val usedPercentage: Int,
    val status: String
)

interface StorageInfoCallback {
    fun onStorageInfoUpdated(storageInfo: StorageInfo)
    fun onStorageInfoError(error: String)
}

class StorageInfoManager(private val context: Context) {

    private val _storageInfo = MutableLiveData<StorageInfo>()
    val storageInfo: LiveData<StorageInfo> = _storageInfo

    fun updateStorageInfo(callback: StorageInfoCallback? = null) {
        try {
            val internalStat = StatFs(Environment.getDataDirectory().path)

            val blockSize = internalStat.blockSizeLong
            val totalBlocks = internalStat.blockCountLong
            val availableBlocks = internalStat.availableBlocksLong

            val totalUserBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = totalUserBytes - availableBytes

            val actualTotalBytes = getTotalDeviceStorageAccurate()
            val displayTotalBytes = max(actualTotalBytes, totalUserBytes)
            val displayFreeBytes = availableBytes
            val displayUsedBytes = displayTotalBytes - displayFreeBytes

            val usedPercentage = if (displayTotalBytes > 0) {
                ((displayUsedBytes.toDouble() / displayTotalBytes.toDouble()) * 100).toInt()
            } else {
                0
            }

            val freeStorageFormatted = formatStorageSize(displayFreeBytes)
            val usedStorageFormatted = formatStorageSize(displayUsedBytes)

            val status = when {
                usedPercentage < 50 -> "Excellent"
                usedPercentage < 80 -> "Good"
                usedPercentage < 95 -> "Warning"
                else -> "Critical"
            }

            val storageInfo = StorageInfo(
                freeStorage = freeStorageFormatted.first,
                usedStorage = usedStorageFormatted.first,
                usedPercentage = usedPercentage,
                status = status
            )

            _storageInfo.postValue(storageInfo)
            callback?.onStorageInfoUpdated(storageInfo)

        } catch (e: Exception) {
            e.printStackTrace()
            val errorInfo = StorageInfo(
                freeStorage = "-- GB",
                usedStorage = "-- GB",
                usedPercentage = 0,
                status = "Unknown"
            )
            _storageInfo.postValue(errorInfo)
            callback?.onStorageInfoError("Failed to get storage information")
        }
    }

    private fun getTotalDeviceStorageAccurate(): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                return storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
            }

            val internalStat = StatFs(Environment.getDataDirectory().path)
            val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong

            val storagePaths = arrayOf(
                Environment.getRootDirectory().absolutePath,
                Environment.getDataDirectory().absolutePath,
                Environment.getDownloadCacheDirectory().absolutePath
            )

            var total: Long = 0
            for (path in storagePaths) {
                val stat = StatFs(path)
                val blockSize = stat.blockSizeLong
                val blockCount = stat.blockCountLong
                total += blockSize * blockCount
            }

            val withSystemOverhead = total + (total * 0.07).toLong()
            max(internalTotal, withSystemOverhead)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val internalStat = StatFs(Environment.getDataDirectory().path)
                val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
                internalTotal + (internalTotal * 0.12).toLong()
            } catch (innerException: Exception) {
                innerException.printStackTrace()
                0L
            }
        }
    }

    private fun formatStorageSize(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1000L * 1000L * 1000L -> {
                val gb = bytes.toDouble() / (1000L * 1000L * 1000L)
                val formatted = if (gb >= 10.0) {
                    DecimalFormat("#").format(gb)
                } else {
                    DecimalFormat("#.#").format(gb)
                }
                Pair("$formatted GB", "GB")
            }

            bytes >= 1000L * 1000L -> {
                val mb = bytes.toDouble() / (1000L * 1000L)
                val formatted = DecimalFormat("#").format(mb)
                Pair("$formatted MB", "MB")
            }

            bytes >= 1000L -> {
                val kb = bytes.toDouble() / 1000L
                val formatted = DecimalFormat("#").format(kb)
                Pair("$formatted KB", "KB")
            }

            else -> {
                Pair("$bytes B", "B")
            }
        }
    }
}