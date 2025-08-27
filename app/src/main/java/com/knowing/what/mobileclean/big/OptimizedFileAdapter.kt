package com.knowing.what.mobileclean.big

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.databinding.ItemFileCleanBinding
import com.knowing.what.mobileclean.ohther.MobileCleanApplication
import java.io.File

interface FileAdapterListener {
    fun onItemClick(position: Int)
    fun onSelectionChanged(selectedCount: Int)
}
class OptimizedFileAdapter(
    private val listener: FileAdapterListener
) : ListAdapter<FileItem, OptimizedFileAdapter.FileViewHolder>(FileItemDiffCallback()) {

    inner class FileViewHolder(
        private val binding: ItemFileCleanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
            binding.ivSelectStatus.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }

        fun bind(fileItem: FileItem) {
            binding.fileItem = fileItem
            binding.executePendingBindings()

            when (fileItem.type) {
                is FileType.Image -> loadImageThumbnail(fileItem.path)
                is FileType.Video -> loadVideoThumbnail(fileItem.path)
                else -> {
                    Glide.with(MobileCleanApplication.instance)
                        .clear(binding.ivFileIcon)
                    binding.ivFileIcon.setImageResource(R.drawable.wenjianjia)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = DataBindingUtil.inflate<ItemFileCleanBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_file_clean,
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: FileViewHolder) {
        super.onViewRecycled(holder)
    }
}

class FileItemDiffCallback : DiffUtil.ItemCallback<FileItem>() {
    override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
        return oldItem == newItem
    }
}