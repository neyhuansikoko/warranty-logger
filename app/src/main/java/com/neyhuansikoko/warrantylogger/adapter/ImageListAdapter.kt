package com.neyhuansikoko.warrantylogger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neyhuansikoko.warrantylogger.database.model.Image
import com.neyhuansikoko.warrantylogger.databinding.ListItemImageBinding

class ImageListAdapter(
    private val onLongClick: (Image) -> Unit,
    private val onClick: (String) -> Unit
) : ListAdapter<Image, ImageListAdapter.ViewHolder>(ImageListAdapter) {

    class ViewHolder(private val binding: ListItemImageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            image: Image,
            onLongClick: (Image) -> Unit,
            onClick: (String) -> Unit
        ) {
            binding.apply {
                imgAddImage.setImageURI(image.thumbnailUri.toUri())
                imgAddImage.setOnLongClickListener {
                    onLongClick(image)
                    return@setOnLongClickListener true
                }
                imgAddImage.setOnClickListener {
                    onClick(image.imageUri)
                }
            }
        }
    }

    companion object DiffCallback: DiffUtil.ItemCallback<Image>() {
        override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem.imageId == newItem.imageId
        }

        override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = getItem(position)
        holder.bind(image, onLongClick, onClick)
    }
}