package com.knowing.what.mobileclean.img

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ActivityShowImageBinding
import com.knowing.what.mobileclean.finish.FinishActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImageListActivity : AppCompatActivity() {
    private val binding by lazy { ActivityShowImageBinding.inflate(layoutInflater) }
    private lateinit var imageGroupAdapter: ImageGroupAdapter
    private val imageGroups = mutableListOf<ImageGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.picture)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }

        initViews()
        loadImages()
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.cbSelectAllGlobal.setOnClickListener {
            toggleSelectAll()
        }

        binding.btnCleanNow.setOnClickListener {
            showDeleteConfirmDialog()
        }

        imageGroupAdapter = ImageGroupAdapter(imageGroups) { updateSelectedSize() }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@ImageListActivity)
            adapter = imageGroupAdapter
        }
    }

    private fun loadImages() {
        val images = getAllImages()
        val groupedImages = groupImagesByDate(images)

        imageGroups.clear()
        imageGroups.addAll(groupedImages)
        imageGroupAdapter.notifyDataSetChanged()

        updateSelectedSize()
    }

    private fun getAllImages(): List<ImageItem> {
        val images = mutableListOf<ImageItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(pathColumn)
                val size = cursor.getLong(sizeColumn)
                val dateTaken = cursor.getLong(dateColumn)

                if (File(path).exists()) {
                    images.add(ImageItem(id, name, path, size, dateTaken))
                }
            }
        }

        return images
    }

    private fun groupImagesByDate(images: List<ImageItem>): List<ImageGroup> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val groupedMap = images.groupBy { image ->
            val date = if (image.dateTaken > 0) Date(image.dateTaken) else Date(File(image.path).lastModified())
            dateFormat.format(date)
        }

        return groupedMap.map { (date, images) ->
            ImageGroup(date, images.toMutableList())
        }.sortedByDescending { it.date }
    }

    private fun toggleSelectAll() {
        val allSelected = imageGroups.all { group ->
            group.images.all { it.isSelected }
        }

        imageGroups.forEach { group ->
            group.images.forEach { image ->
                image.isSelected = !allSelected
            }
        }

        imageGroupAdapter.notifyDataSetChanged()
        updateSelectAllIcon()
        updateSelectedSize()
    }

    private fun updateSelectAllIcon() {
        val allSelected = imageGroups.all { group ->
            group.images.all { it.isSelected }
        }

        binding.cbSelectAllGlobal.setImageResource(
            if (allSelected) R.drawable.ic_check else R.drawable.ic_discheck
        )
    }

    private fun updateSelectedSize() {
        val totalSize = imageGroups.sumOf { group ->
            group.images.filter { it.isSelected }.sumOf { it.size }
        }

        val (size, unit) = formatFileSize(totalSize)
        binding.tvScannedSize.text = size
        binding.tvScannedSizeUn.text = unit

        updateSelectAllIcon()
    }

    private fun formatFileSize(bytes: Long): Pair<String, String> {
        return when {
            bytes < 1024 -> Pair(bytes.toString(), "B")
            bytes < 1024 * 1024 -> Pair(String.format("%.1f", bytes / 1024.0), "KB")
            bytes < 1024 * 1024 * 1024 -> Pair(String.format("%.1f", bytes / (1024.0 * 1024.0)), "MB")
            else -> Pair(String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0)), "GB")
        }
    }

    private fun showDeleteConfirmDialog() {
        val selectedImages = imageGroups.flatMap { group ->
            group.images.filter { it.isSelected }
        }

        if (selectedImages.isEmpty()) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete confirmation")
            .setMessage("Are you sure you want to delete the selected ${selectedImages.size} images?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedImages(selectedImages)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedImages(selectedImages: List<ImageItem>) {
        var deletedCount = 0

        selectedImages.forEach { image ->
            try {
                val file = File(image.path)
                if (file.exists() && file.delete()) {
                    contentResolver.delete(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "${MediaStore.Images.Media._ID} = ?",
                        arrayOf(image.id.toString())
                    )
                    deletedCount++
                }
            } catch (e: Exception) {
                Log.e("ImageListActivity", "Failed to delete image: ${image.path}", e)
            }
        }
        val totalSize = imageGroups.sumOf { group ->
            group.images.filter { it.isSelected }.sumOf { it.size }
        }
        val intent = Intent(this, FinishActivity::class.java)
        intent.putExtra("CLEANED_SIZE", totalSize)
        intent.putExtra("jump_type", "image")
        startActivity(intent)
        finish()

    }
}

data class ImageItem(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val dateTaken: Long,
    var isSelected: Boolean = false
)

data class ImageGroup(
    val date: String,
    val images: MutableList<ImageItem>
)