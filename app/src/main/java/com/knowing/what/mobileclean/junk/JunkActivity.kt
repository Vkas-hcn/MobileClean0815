package com.knowing.what.mobileclean.junk


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ActivityJunkBinding
import com.knowing.what.mobileclean.databinding.ItemCategoryBinding
import com.knowing.what.mobileclean.databinding.ItemFileBinding
import com.knowing.what.mobileclean.finish.FinishActivity
import kotlin.reflect.KProperty


class JunkActivity : AppCompatActivity() {

    private val binding: ActivityJunkBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_junk)
    }

    private val viewModel: JunkCleanViewModel by viewModels {
        JunkCleanViewModelFactory(
            JunkFileRepository(this)
        )
    }

    private val categoryAdapter by lazy {
        CategoryAdapter(
            onCategoryClick = viewModel::toggleCategoryExpansion,
            onCategorySelectClick = viewModel::toggleCategorySelection,
            onFileSelectClick = viewModel::toggleFileSelection
        )
    }

    private var uiState by UiStateDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupWindowInsets()
        setupViews()
        setupObservers()

        viewModel.startScan()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() {
        supportActionBar?.hide()

        binding.apply {
            lifecycleOwner = this@JunkActivity
            viewModel = this@JunkActivity.viewModel
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@JunkActivity)
            adapter = categoryAdapter
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCleanNow.setOnClickListener { viewModel.startClean() }

        uiState = UiState.Scanning
    }

    private fun setupObservers() {
        viewModel.scanState.observe(this) { state ->
            handleScanState(state)
        }

        viewModel.cleanState.observe(this) { state ->
            handleCleanState(state)
        }

        viewModel.categories.observe(this) { categories ->
            categoryAdapter.submitList(categories) {
                categoryAdapter.notifyDataSetChanged()
            }
        }

        viewModel.formattedTotalSize.observe(this) { formattedSize ->
            updateSizeDisplay(formattedSize)
        }

        viewModel.formattedSelectedSize.observe(this) { selectedSize ->
            binding.btnCleanNow.isEnabled = selectedSize != "0.0 KB"
        }
    }

    private fun handleScanState(state: ScanState) {
        when (state) {
            is ScanState.Scanning -> {
                uiState = UiState.Scanning
                binding.apply {
                    progressScaning.visibility = View.VISIBLE
                    progressScaning.progress = state.progress
                    tvScanningPath.text = "Scanning: ${state.currentPath}"
                    btnCleanNow.visibility = View.GONE
                }
            }
            is ScanState.Completed -> {
                uiState = UiState.ScanCompleted
                binding.apply {
                    progressScaning.visibility = View.GONE
                    tvScanningPath.text = "Scan completed - Found ${state.totalFiles} files"
                    btnCleanNow.visibility = if (state.totalFiles > 0) View.VISIBLE else View.GONE

                    // 更新背景图片
                    if (state.totalSize > 0) {
                        imgScanBg.setImageResource(R.drawable.bg_junk)
                    }
                }

                refreshCategoryList()
            }
            is ScanState.Error -> {
                uiState = UiState.Error
                binding.apply {
                    progressScaning.visibility = View.GONE
                    tvScanningPath.text = "Scan error: ${state.error.message}"
                }
                Toast.makeText(this, "Scan failed: ${state.error.message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
    private fun refreshCategoryList() {
        binding.rvCategories.postDelayed({
            categoryAdapter.notifyDataSetChanged()
        }, 100)
    }

    private fun handleCleanState(state: CleanState) {
        when (state) {
            is CleanState.Cleaning -> {
                uiState = UiState.Cleaning
                binding.apply {
                    tvScanningPath.text = "Cleaning: ${state.currentFile}"
                    btnCleanNow.isEnabled = false
                }
            }
            is CleanState.Completed -> {
                uiState = UiState.CleanCompleted
                Toast.makeText(
                    this,
                    "Cleaned ${state.deletedCount} files (${FileUtils.formatFileSize(state.deletedSize)})",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this, FinishActivity::class.java)
                intent.putExtra("CLEANED_SIZE", state.deletedSize)
                intent.putExtra("jump_type", "junk")
                startActivity(intent)
                finish()
            }
            is CleanState.Error -> {
                uiState = UiState.Error
                binding.btnCleanNow.isEnabled = true
                Toast.makeText(this, "Clean failed: ${state.error.message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    private fun updateSizeDisplay(formattedSize: String) {
        val parts = formattedSize.split(" ")
        if (parts.size == 2) {
            binding.tvScannedSize.text = parts[0]
            binding.tvScannedSizeUn.text = parts[1]
        } else {
            binding.tvScannedSize.text = formattedSize
            binding.tvScannedSizeUn.text = ""
        }
    }
}


enum class UiState {
    Idle, Scanning, ScanCompleted, Cleaning, CleanCompleted, Error
}


class UiStateDelegate {
    private var state: UiState = UiState.Idle

    operator fun getValue(thisRef: Any?, property: KProperty<*>): UiState = state

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: UiState) {
        if (state != value) {
            state = value
        }
    }
}


class CategoryAdapter(
    private val onCategoryClick: (JunkCategory) -> Unit,
    private val onCategorySelectClick: (JunkCategory) -> Unit,
    private val onFileSelectClick: (JunkFile, JunkCategory) -> Unit
) : ListAdapter<JunkCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val fileAdapter by lazy {
            FileAdapter { file ->
                val category = getItem(adapterPosition)
                onFileSelectClick(file, category)
            }
        }

        init {
            binding.rvItemFile.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = fileAdapter
            }

            binding.llCategory.setOnClickListener {
                val category = getItem(adapterPosition)
                onCategoryClick(category)
            }

            binding.imgSelect.setOnClickListener {
                val category = getItem(adapterPosition)
                onCategorySelectClick(category)
            }
        }

        fun bind(category: JunkCategory) {
            binding.category = category

            binding.apply {
                ivIcon.setImageResource(category.iconRes)

                imgSelect.setImageResource(
                    if (category.isSelected) R.drawable.ic_check else R.drawable.ic_discheck
                )

                imgInstruct.setImageResource(
                    if (category.isExpanded) R.drawable.ic_bleow else R.drawable.ic_right
                )

                rvItemFile.visibility = if (category.isExpanded) View.VISIBLE else View.GONE

                tvTitle.text = category.name
                tvFileCount.text = "${category.files.size} files"
                tvSize.text = category.formattedTotalSize

                // 更新文件列表
                if (category.isExpanded) {
                    fileAdapter.submitList(category.files.toList()) {
                        rvItemFile.visibility = View.VISIBLE
                        fileAdapter.notifyDataSetChanged()
                    }
                }

                executePendingBindings()
            }
        }
    }
}


class CategoryDiffCallback : DiffUtil.ItemCallback<JunkCategory>() {
    override fun areItemsTheSame(oldItem: JunkCategory, newItem: JunkCategory): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: JunkCategory, newItem: JunkCategory): Boolean {
        return oldItem == newItem &&
                oldItem.files.size == newItem.files.size &&
                oldItem.isExpanded == newItem.isExpanded &&
                oldItem.isSelected == newItem.isSelected
    }
}


class FileAdapter(
    private val onFileClick: (JunkFile) -> Unit
) : ListAdapter<JunkFile, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val file = getItem(position)
            holder.updateSelectionState(file)
        }
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val file = getItem(adapterPosition)
                    onFileClick(file)
                }
            }

            binding.imgFileSelect.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val file = getItem(adapterPosition)
                    onFileClick(file)
                }
            }
        }

        fun bind(file: JunkFile) {
            binding.file = file

            updateSelectionState(file)

            binding.executePendingBindings()
        }

        fun updateSelectionState(file: JunkFile) {
            binding.imgFileSelect.setImageResource(
                if (file.isSelected) R.drawable.ic_check else R.drawable.ic_discheck
            )
        }
    }
}

class FileDiffCallback : DiffUtil.ItemCallback<JunkFile>() {
    override fun areItemsTheSame(oldItem: JunkFile, newItem: JunkFile): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: JunkFile, newItem: JunkFile): Boolean {
        return oldItem == newItem && oldItem.isSelected == newItem.isSelected
    }

    override fun getChangePayload(oldItem: JunkFile, newItem: JunkFile): Any? {
        return if (oldItem.isSelected != newItem.isSelected) {
            "selection_changed"
        } else {
            null
        }
    }
}