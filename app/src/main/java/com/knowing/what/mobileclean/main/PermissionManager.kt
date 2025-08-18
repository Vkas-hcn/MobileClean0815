package com.knowing.what.mobileclean.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface PermissionCallback {
    fun onPermissionGranted()
    fun onPermissionDenied(shouldShowRationale: Boolean)
}

class PermissionDelegate(
    private val activity: FragmentActivity,
    private val callback: PermissionCallback
) : ReadOnlyProperty<Any?, PermissionManager> {

    private var permissionManager: PermissionManager? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): PermissionManager {
        return permissionManager ?: PermissionManager(activity, callback).also {
            permissionManager = it
        }
    }
}

class PermissionManager(
    private val activity: FragmentActivity,
    private val callback: PermissionCallback
) {
    private val sharedPreferences = activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)

    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionResult(permissions)
        }

    private val manageStorageLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (hasStoragePermission()) {
                callback.onPermissionGranted()
            } else {
                callback.onPermissionDenied(false)
            }
        }

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestManageExternalStoragePermission()
        } else {
            requestTraditionalStoragePermission()
        }
    }

    fun showPermissionDeniedDialog() {
        val deniedCount = getPermissionDeniedCount()
        incrementPermissionDeniedCount()

        if (deniedCount == 0) {
            showSimplePermissionDeniedDialog()
        } else {
            showDetailedPermissionDeniedDialog()
        }
    }

    private fun requestManageExternalStoragePermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            manageStorageLauncher.launch(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            } catch (ex: Exception) {
                openAppSettings()
            }
        }
    }

    private fun requestTraditionalStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissionLauncher.launch(permissions)
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            callback.onPermissionGranted()
        } else {
            val shouldShowRationale = permissions.keys.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            callback.onPermissionDenied(shouldShowRationale)
        }
    }

    private fun showSimplePermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Requires storage permissions")
            .setMessage("To clean up your device, the app needs access to storage.")
            .setPositiveButton("Re-authorization") { _, _ ->
                requestStoragePermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDetailedPermissionDeniedDialog() {
        val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "The application requires \"Manage All Files\" permission to clean up your device. Please find this app in the settings and enable the \"Allow management of all files\" permission."
        } else {
            "The app requires storage permission to clean up your device. Please find the app in settings and enable the \"Storage\" permission."
        }

        AlertDialog.Builder(activity)
            .setTitle("Requires storage permissions")
            .setMessage(message)
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            try {
                activity.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getPermissionDeniedCount(): Int {
        return sharedPreferences.getInt("permission_denied_count", 0)
    }

    private fun incrementPermissionDeniedCount() {
        val currentCount = getPermissionDeniedCount()
        sharedPreferences.edit().putInt("permission_denied_count", currentCount + 1).apply()
    }
}