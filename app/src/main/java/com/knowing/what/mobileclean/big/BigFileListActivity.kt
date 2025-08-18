package com.knowing.what.mobileclean.big

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.knowing.what.mobileclean.databinding.ActivityBigFileBinding
import com.knowing.what.mobileclean.finish.FinishActivity

class BigFileListActivity : AppCompatActivity(), FileAdapterListener {

    private val binding by lazy { ActivityBigFileBinding.inflate(layoutInflater) }

    private lateinit var adapter: OptimizedFileAdapter


    private val viewModel: ImprovedBigFileViewModel by viewModels {
        ImprovedBigFileViewModelFactory(
            scanFilesUseCase = ScanFilesUseCase(FileRepositoryImpl(this)),
            deleteFilesUseCase = DeleteFilesUseCase(FileRepositoryImpl(this)),
            filterFilesUseCase = FilterFilesUseCase()
        )
    }

    private val filterOptions by lazy {
        mapOf(
            "types" to arrayOf("All types", "Image", "Video", "Audio", "Docs", "Download", "Zip"),
            "sizes" to arrayOf("All Size", ">1MB", ">5MB", ">10MB", ">20MB", ">50MB", ">100MB", ">200MB", ">500MB"),
            "times" to arrayOf("All Time", "Within 1 day", "Within 1 week", "Within 1 month", "Within 3 month", "Within 6 month")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(binding.root)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setupViews()
        observeViewModel()


        viewModel.startScanning()
    }

    private fun setupViews() {
        binding.textBack.setOnClickListener { finish() }

        adapter = OptimizedFileAdapter(this)
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@BigFileListActivity)
            adapter = this@BigFileListActivity.adapter

            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.tvType.setOnClickListener { showFilterDialog("types") }
        binding.tvSize.setOnClickListener { showFilterDialog("sizes") }
        binding.tvTime.setOnClickListener { showFilterDialog("times") }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.textBack.setOnLongClickListener {
            showSelectionOptionsDialog()
            true
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            adapter.submitList(state.files) {
                if (state.files.isNotEmpty()) {
                    binding.rvFiles.scrollToPosition(0)
                }
            }

            updateVisibility(state)
            updateDeleteButton(state)
            updateFilterDisplay(state)
            handleError(state)
        }

        viewModel.navigateToFinish.observe(this) { result ->
            if (result != null) {
                navigateToFinishActivity(result)
                viewModel.onNavigatedToFinish()
            }
        }
    }

    private fun updateVisibility(state: BigFileUiState) {
        binding.tvNoFiles.visibility = if (state.files.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
        binding.rvFiles.visibility = if (state.files.isEmpty() && !state.isLoading) View.INVISIBLE else View.VISIBLE
    }

    private fun updateDeleteButton(state: BigFileUiState) {
        binding.btnDelete.isEnabled = state.selectedCount > 0
        binding.btnDelete.text = if (state.selectedCount > 0) "Delete (${state.selectedCount})" else "Delete"
    }

    private fun updateFilterDisplay(state: BigFileUiState) {
        binding.tvType.text = state.filter.type
        binding.tvSize.text = state.filter.size
        binding.tvTime.text = state.filter.time
    }

    private fun handleError(state: BigFileUiState) {
        state.error?.let { message ->
            if (message.startsWith("Successfully")) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showFilterDialog(filterType: String) {
        val options = filterOptions[filterType] ?: return
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Select ${filterType.removeSuffix("s")}")
            .setItems(options) { _, which ->
                val selectedOption = options[which]
                when(filterType) {
                    "types" -> viewModel.updateFilter(type = selectedOption)
                    "sizes" -> viewModel.updateFilter(size = selectedOption)
                    "times" -> viewModel.updateFilter(time = selectedOption)
                }
            }
            .create()

        alertDialog.show()
    }

    private fun showDeleteConfirmDialog() {
        val selectedCount = viewModel.uiState.value?.selectedCount ?: 0
        if (selectedCount == 0) return

        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete $selectedCount file(s)?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSelectedFiles()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSelectionOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Selection Options")
            .setItems(arrayOf("Select All", "Clear All")) { _, which ->
                when(which) {
                    0 -> viewModel.selectAllFiles()
                    1 -> viewModel.clearAllSelections()
                }
            }
            .show()
    }

    private fun navigateToFinishActivity(result: DeleteResult) {
        val intent = Intent(this, FinishActivity::class.java)
        intent.putExtra("CLEANED_SIZE", result.totalSize)
        intent.putExtra("jump_type", "file")
        startActivity(intent)
        finish()
    }

    override fun onItemClick(position: Int) {
        viewModel.toggleFileSelection(position)
    }

    override fun onSelectionChanged(selectedCount: Int) {
    }
}
