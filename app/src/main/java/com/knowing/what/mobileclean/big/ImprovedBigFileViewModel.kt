package com.knowing.what.mobileclean.big

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class ImprovedBigFileViewModel(
    private val scanFilesUseCase: ScanFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val filterFilesUseCase: FilterFilesUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData(BigFileUiState())
    val uiState: LiveData<BigFileUiState> = _uiState

    private val _navigateToFinish = MutableLiveData<DeleteResult?>()
    val navigateToFinish: LiveData<DeleteResult?> = _navigateToFinish

    private val allFiles = mutableListOf<FileItem>()

    private var currentFilter by Delegates.observable(FileFilter()) { _, _, newValue ->
        applyFilters(newValue)
    }

    private val selectedFiles by lazy {
        mutableSetOf<String>()
    }

    fun startScanning() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)

            try {
                val files = scanFilesUseCase()
                allFiles.clear()
                allFiles.addAll(files)
                applyFilters(currentFilter)
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateFilter(type: String? = null, size: String? = null, time: String? = null) {
        currentFilter = currentFilter.copy(
            type = type ?: currentFilter.type,
            size = size ?: currentFilter.size,
            time = time ?: currentFilter.time
        )
    }

    fun toggleFileSelection(position: Int) {
        val currentFiles = _uiState.value?.files?.toMutableList() ?: return
        if (position !in currentFiles.indices) return

        val file = currentFiles[position]
        val newSelectionState = !file.isSelected

        if (newSelectionState) {
            selectedFiles.add(file.path)
        } else {
            selectedFiles.remove(file.path)
        }

        currentFiles[position] = file.copy(isSelected = newSelectionState)

        val selectedCount = currentFiles.count { it.isSelected }
        _uiState.value = _uiState.value?.copy(
            files = currentFiles,
            selectedCount = selectedCount
        )
    }

    fun selectAllFiles() {
        val currentFiles = _uiState.value?.files?.map { file ->
            selectedFiles.add(file.path)
            file.copy(isSelected = true)
        } ?: return

        _uiState.value = _uiState.value?.copy(
            files = currentFiles,
            selectedCount = currentFiles.size
        )
    }

    fun clearAllSelections() {
        selectedFiles.clear()
        val currentFiles = _uiState.value?.files?.map { file ->
            file.copy(isSelected = false)
        } ?: return

        _uiState.value = _uiState.value?.copy(
            files = currentFiles,
            selectedCount = 0
        )
    }

    fun deleteSelectedFiles() {
        val selectedFiles = _uiState.value?.files?.filter { it.isSelected } ?: return
        if (selectedFiles.isEmpty()) return

        viewModelScope.launch {
            try {
                val result = deleteFilesUseCase(selectedFiles)
                if (result.success) {
                    startScanning()
                    _navigateToFinish.value = result
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(error = e.message)
            }
        }
    }

    fun onNavigatedToFinish() {
        _navigateToFinish.value = null
    }

    private fun applyFilters(filter: FileFilter) {
        val filteredFiles = filterFilesUseCase(allFiles, filter)

        val updatedFiles = filteredFiles.map { file ->
            file.copy(isSelected = selectedFiles.contains(file.path))
        }

        _uiState.value = _uiState.value?.copy(
            files = updatedFiles,
            filter = filter,
            selectedCount = updatedFiles.count { it.isSelected },
            isLoading = false
        )
    }
}

class ImprovedBigFileViewModelFactory(
    private val scanFilesUseCase: ScanFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val filterFilesUseCase: FilterFilesUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImprovedBigFileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImprovedBigFileViewModel(
                scanFilesUseCase,
                deleteFilesUseCase,
                filterFilesUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
