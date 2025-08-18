package com.knowing.what.mobileclean.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(
    private val storageInfoManager: StorageInfoManager,
    private val navigationManager: NavigationManager
) : ViewModel(), StorageInfoCallback, NavigationCallback {

    val storageInfo = storageInfoManager.storageInfo

    private var pendingNavigationAction: NavigationAction? = null
    var onPermissionRequired: (() -> Unit)? = null
    var onNavigationReady: ((NavigationAction) -> Unit)? = null

    fun updateStorageInfo() {
        viewModelScope.launch {
            storageInfoManager.updateStorageInfo(this@MainViewModel)
        }
    }

    fun requestNavigation(action: NavigationAction, hasPermission: Boolean) {
        if (hasPermission) {
            navigationManager.navigate(action)
        } else {
            pendingNavigationAction = action
            onPermissionRequired?.invoke()
        }
    }

    fun onPermissionGranted() {
        pendingNavigationAction?.let { action ->
            navigationManager.navigate(action)
            pendingNavigationAction = null
        }
    }

    override fun onStorageInfoUpdated(storageInfo: StorageInfo) {
    }

    override fun onStorageInfoError(error: String) {
    }

    override fun onNavigationRequested(action: NavigationAction) {
        onNavigationReady?.invoke(action)
    }
}