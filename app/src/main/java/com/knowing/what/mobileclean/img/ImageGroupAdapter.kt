package com.knowing.what.mobileclean.img

import com.knowing.what.mobileclean.R


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ImageGroupAdapter(
    private val imageGroups: List<ImageGroup>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<ImageGroupAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_show_picture_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageGroups[position])
    }

    override fun getItemCount() = imageGroups.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val cbSelectAll: ImageView = itemView.findViewById(R.id.cb_select_all)
        private val rvPictures: RecyclerView = itemView.findViewById(R.id.rv_pictures)

        fun bind(imageGroup: ImageGroup) {
            tvDate.text = imageGroup.date

            val allSelected = imageGroup.images.all { it.isSelected }
            cbSelectAll.setImageResource(
                if (allSelected && imageGroup.images.isNotEmpty()) R.drawable.ic_check
                else R.drawable.ic_discheck
            )

            cbSelectAll.setOnClickListener {
                val newState = !allSelected
                imageGroup.images.forEach { it.isSelected = newState }
                notifyItemChanged(adapterPosition)
                onSelectionChanged()
            }

            val imageAdapter = ImageAdapter(imageGroup.images) {
                notifyItemChanged(adapterPosition)
                onSelectionChanged()
            }

            rvPictures.apply {
                layoutManager = GridLayoutManager(itemView.context, 4)
                adapter = imageAdapter
            }
        }
    }
}