package com.knowing.what.mobileclean.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(
    private val storageInfoManager: StorageInfoManager,
    private val navigationManager: NavigationManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(storageInfoManager, navigationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}