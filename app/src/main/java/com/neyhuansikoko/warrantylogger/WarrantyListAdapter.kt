package com.neyhuansikoko.warrantylogger

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neyhuansikoko.warrantylogger.database.Warranty
import com.neyhuansikoko.warrantylogger.database.getRemainingDays
import com.neyhuansikoko.warrantylogger.databinding.ListItemWarrantyBinding

class WarrantyListAdapter(
    private val clickListener: (Warranty) -> Unit,
    private val contextListener: (Warranty, Boolean) -> Unit
) : ListAdapter<Warranty, WarrantyListAdapter.ViewHolder>(DiffCallback) {

    var selectAll: Boolean = false

    class ViewHolder(private val binding: ListItemWarrantyBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(warranty: Warranty) {
            binding.apply {
                tvItemWarrantyName.text = warranty.warrantyName
                tvItemExpirationDate.text = formatDateMillis(warranty.expirationDate)
                tvItemStatus.text = warranty.getRemainingDays()
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
        holder.itemView.findViewById<CheckBox>(R.id.cb_item_delete).apply {
            isChecked = selectAll
            setOnClickListener {
                contextListener(warranty, isChecked)
            }
        }

        holder.bind(warranty)
    }

    fun setSelectAll() {
        selectAll = true
        notifyDataSetChanged()
    }

    fun setUnselectAll() {
        selectAll = false
        notifyDataSetChanged()
    }
}