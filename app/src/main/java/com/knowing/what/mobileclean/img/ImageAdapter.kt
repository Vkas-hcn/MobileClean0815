package com.knowing.what.mobileclean.img


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.knowing.what.mobileclean.R
import com.knowing.what.mobileclean.ohther.MobileCleanApplication
import java.io.File

class ImageAdapter(
    private val images: List<ImageItem>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_show_picture, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount() = images.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPicture: ImageView = itemView.findViewById(R.id.iv_picture)
        private val cbSelect: ImageView = itemView.findViewById(R.id.cb_select)
        private val tvSize: MaterialButton = itemView.findViewById(R.id.tv_size)

        fun bind(image: ImageItem) {
            Glide.with(MobileCleanApplication.instance)
                .load(File(image.path))
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(ivPicture)

            cbSelect.setImageResource(
                if (image.isSelected) R.drawable.ic_check else R.drawable.ic_discheck
            )

            tvSize.text = formatFileSize(image.size)

            itemView.setOnClickListener {
                image.isSelected = !image.isSelected
                cbSelect.setImageResource(
                    if (image.isSelected) R.drawable.ic_check else R.drawable.ic_discheck
                )
                onSelectionChanged()
            }

            cbSelect.setOnClickListener {
                image.isSelected = !image.isSelected
                cbSelect.setImageResource(
                    if (image.isSelected) R.drawable.ic_check else R.drawable.ic_discheck
                )
                onSelectionChanged()
            }
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "${bytes}B"
                bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
                bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))}MB"
                else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))}GB"
            }
        }
    }
}