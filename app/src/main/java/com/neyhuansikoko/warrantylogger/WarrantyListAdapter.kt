package com.neyhuansikoko.warrantylogger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.databinding.ListItemWarrantyBinding

class WarrantyListAdapter(
    private val clickListener: (Warranty) -> Unit
) : ListAdapter<Warranty, WarrantyListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ListItemWarrantyBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(warranty: Warranty) {
            binding.apply {
                tvItemWarrantyName.text = warranty.warrantyName
                tvItemExpirationDate.text = formatDateMillis(warranty.expirationDate)
                tvItemStatus.text = warranty.getRemainingTime()
            }
        }
    }

    companion object DiffCallback: DiffUtil.ItemCallback<Warranty>() {
        override fun areItemsTheSame(oldItem: Warranty, newItem: Warranty): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Warranty, newItem: Warranty): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ListItemWarrantyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val warranty = getItem(position)
        holder.itemView.setOnClickListener { clickListener(warranty) }
        holder.bind(warranty)
    }
}