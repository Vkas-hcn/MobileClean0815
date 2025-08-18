package com.knowing.what.mobileclean.big


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ItemFileCleanBinding
import com.knowing.what.mobileclean.ohther.MobileCleanApplication
import java.io.File

class FileAdapter(
    private val files: MutableList<FileItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(var binding: ItemFileCleanBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileItem, position: Int) {
            binding.tvFileName.text = file.name
            binding.tvFileSize.text = formatFileSize(file.size)
            binding.ivSelectStatus.setImageResource(if(file.isSelected){R.drawable.ic_check}else{R.drawable.ic_discheck})
            binding.root.setOnClickListener {
                onItemClick(position)
            }
            binding.ivSelectStatus.setOnClickListener {
                onItemClick(position)
            }
            when (file.type) {
                FileType.Image -> {
                    loadImageThumbnail(file.path)
                }

                FileType.Video -> {
                    loadVideoThumbnail(file.path)
                }

                else -> {
                    Glide.with(MobileCleanApplication.instance).clear(binding.ivFileIcon)
                    binding.ivFileIcon.setImageResource(getFileIcon(file.type))
                }
            }
        }

        private fun loadImageThumbnail(imagePath: String) {
            val requestOptions = RequestOptions()
                .override(200, 200)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.weikong)
                .error(R.drawable.weikong)

            Glide.with(MobileCleanApplication.instance)
                .load(File(imagePath))
                .apply(requestOptions)
                .into(binding.ivFileIcon)
        }

        private fun loadVideoThumbnail(videoPath: String) {
            val requestOptions = RequestOptions()
                .override(200, 200)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.weikong)
                .error(R.drawable.weikong)

            Glide.with(MobileCleanApplication.instance)
                .load(File(videoPath))
                .apply(requestOptions)
                .into(binding.ivFileIcon)
        }

        @SuppressLint("DefaultLocale")
        private fun formatFileSize(size: Long): String {
            return when {
                size >= 1024 * 1024 * 1024 -> String.format(
                    "%.1f GB",
                    size / (1024.0 * 1024.0 * 1024.0)
                )

                size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                else -> String.format("%.1f KB", size / 1024.0)
            }
        }

        private fun getFileIcon(type: FileType): Int {
            return R.drawable.wenjianjia
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileCleanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position], position)
    }

    override fun getItemCount(): Int = files.size

    override fun onViewRecycled(holder: FileViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(MobileCleanApplication.instance).clear(holder.binding.ivFileIcon)
    }
}