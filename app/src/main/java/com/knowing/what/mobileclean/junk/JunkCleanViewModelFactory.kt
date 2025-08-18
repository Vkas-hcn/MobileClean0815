package com.knowing.what.mobileclean.junk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class JunkCleanViewModelFactory(
    private val repository: JunkFileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JunkCleanViewModel::class.java)) {
            return JunkCleanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}