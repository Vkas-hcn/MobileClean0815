package com.knowing.what.mobileclean.junk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class JunkCleanViewModel(
    private val repository: JunkFileRepository
) : ViewModel() {

    private val _scanState = MutableLiveData<ScanState>()
    val scanState: LiveData<ScanState> = _scanState

    private val _cleanState = MutableLiveData<CleanState>()
    val cleanState: LiveData<CleanState> = _cleanState

    private val _categories = MutableLiveData<List<JunkCategory>>()
    val categories: LiveData<List<JunkCategory>> = _categories

    private val _totalSize = MutableLiveData<Long>()
    val totalSize: LiveData<Long> = _totalSize

    private val _selectedSize = MutableLiveData<Long>()
    val selectedSize: LiveData<Long> = _selectedSize

    val formattedTotalSize: LiveData<String> = totalSize.map {
        FileUtils.formatFileSize(it)
    }

    val formattedSelectedSize: LiveData<String> = selectedSize.map {
        FileUtils.formatFileSize(it)
    }

    private var isScanning = false

    init {
        initializeCategories()
    }


    fun startScan() {
        if (isScanning) return

        isScanning = true
        _scanState.value = ScanState.Scanning(0, "")

        viewModelScope.launch {
            repository.scanJunkFiles(object : ScanProgressListener {
                override fun onScanStarted() {
                    _scanState.postValue(ScanState.Scanning(0, "Starting scan..."))
                }

                override fun onScanProgress(progress: Int, currentPath: String) {
                    _scanState.postValue(ScanState.Scanning(progress, currentPath))
                }

                override fun onFileFound(junkFile: JunkFile) {
                    addJunkFile(junkFile)
                }

                override fun onScanCompleted(totalFiles: Int, totalSize: Long) {
                    isScanning = false
                    _scanState.postValue(ScanState.Completed(totalFiles, totalSize))
                    selectAllFiles()
                    expandCategoriesWithFiles()
                }

                override fun onScanError(error: Throwable) {
                    isScanning = false
                    _scanState.postValue(ScanState.Error(error))
                }
            })
        }
    }


    fun startClean() {
        val selectedFiles = getSelectedFiles()
        if (selectedFiles.isEmpty()) return

        _cleanState.value = CleanState.Cleaning(0, "")

        viewModelScope.launch {
            repository.cleanFiles(selectedFiles, object : CleanProgressListener {
                override fun onCleanStarted() {
                    _cleanState.postValue(CleanState.Cleaning(0, "Starting clean..."))
                }

                override fun onCleanProgress(progress: Int, currentFile: String) {
                    _cleanState.postValue(CleanState.Cleaning(progress, currentFile))
                }

                override fun onCleanCompleted(deletedCount: Int, deletedSize: Long) {
                    _cleanState.postValue(CleanState.Completed(deletedCount, deletedSize))
                }

                override fun onCleanError(error: Throwable) {
                    _cleanState.postValue(CleanState.Error(error))
                }
            })
        }
    }


    fun toggleCategorySelection(category: JunkCategory) {
        val currentCategories = _categories.value?.toMutableList() ?: return
        val index = currentCategories.indexOfFirst { it.type == category.type }
        if (index != -1) {
            val updatedCategory = currentCategories[index].copy(
                isSelected = !category.isSelected
            )
            updatedCategory.files.forEach { it.isSelected = updatedCategory.isSelected }
            currentCategories[index] = updatedCategory
            _categories.value = currentCategories
            updateSelectedSize()
        }
    }


    fun toggleCategoryExpansion(category: JunkCategory) {
        val currentCategories = _categories.value?.toMutableList() ?: return
        val index = currentCategories.indexOfFirst { it.type == category.type }
        if (index != -1) {
            currentCategories[index] = currentCategories[index].copy(
                isExpanded = !category.isExpanded
            )
            _categories.value = currentCategories
        }
    }


    fun toggleFileSelection(file: JunkFile, category: JunkCategory) {
        val currentCategories = _categories.value?.toMutableList() ?: return
        val categoryIndex = currentCategories.indexOfFirst { it.type == category.type }
        if (categoryIndex != -1) {
            val fileIndex = currentCategories[categoryIndex].files.indexOfFirst { it.path == file.path }
            if (fileIndex != -1) {
                currentCategories[categoryIndex].files[fileIndex].isSelected = !file.isSelected
                val hasSelectedFiles = currentCategories[categoryIndex].files.any { it.isSelected }
                currentCategories[categoryIndex] = currentCategories[categoryIndex].copy(
                    isSelected = hasSelectedFiles
                )
                _categories.value = currentCategories
                updateSelectedSize()
            }
        }
    }

    private fun initializeCategories() {
        val initialCategories = JunkFileTypeMy.values().map { type ->
            JunkCategory(type = type)
        }
        _categories.value = initialCategories
        _totalSize.value = 0L
        _selectedSize.value = 0L
    }

    private fun addJunkFile(junkFile: JunkFile) {
        val currentCategories = _categories.value?.toMutableList() ?: return
        val categoryIndex = currentCategories.indexOfFirst { it.type == junkFile.type }

        if (categoryIndex != -1) {
            currentCategories[categoryIndex].files.add(junkFile)
            _categories.value = currentCategories

            val newTotalSize = (_totalSize.value ?: 0L) + junkFile.size
            _totalSize.value = newTotalSize
        }
    }

    private fun selectAllFiles() {
        val currentCategories = _categories.value?.toMutableList() ?: return
        var hasChanges = false

        currentCategories.forEach { category ->
            category.files.forEach { file ->
                if (!file.isSelected) {
                    file.isSelected = true
                    hasChanges = true
                }
            }
            if (category.files.isNotEmpty() && !category.isSelected) {
                category.isSelected = true
                hasChanges = true
            }
        }

        if (hasChanges) {
            _categories.value = currentCategories
            updateSelectedSize()
        }
    }


    private fun expandCategoriesWithFiles() {
        val currentCategories = _categories.value?.toMutableList() ?: return
        var hasChanges = false

        currentCategories.forEach { category ->
            if (category.files.isNotEmpty() && !category.isExpanded) {
                category.isExpanded = true
                hasChanges = true
            }
        }

        if (hasChanges) {
            _categories.value = currentCategories
        }
    }

    private fun updateSelectedSize() {
        val selectedSize = _categories.value?.sumOf { it.selectedSize } ?: 0L
        _selectedSize.value = selectedSize
    }

    private fun getSelectedFiles(): List<JunkFile> {
        return _categories.value?.flatMap { category ->
            category.files.filter { it.isSelected }
        } ?: emptyList()
    }
}