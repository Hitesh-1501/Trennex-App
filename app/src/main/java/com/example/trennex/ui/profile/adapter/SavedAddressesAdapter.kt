package com.example.trennex.ui.profile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemSavedAddressProfileBinding
import com.example.trennex.repository.user.AddressEntity

class SavedAddressesAdapter(
    private val onEditClick: (AddressEntity) -> Unit,
    private val onDeleteClick: (AddressEntity) -> Unit
) : ListAdapter<AddressEntity, SavedAddressesAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemSavedAddressProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AddressEntity, onEditClick: (AddressEntity) -> Unit, onDeleteClick: (AddressEntity) -> Unit) {
            binding.tvUserName.text = item.userName
            binding.tvFullAddress.text = item.displayAddress
            binding.tvMobile.text = item.mobile.ifBlank { "N/A" }

            binding.ivAddressIcon.setImageResource(
                if (item.addressType.equals("Office", ignoreCase = true)) {
                    R.drawable.ic_office
                } else {
                    R.drawable.ic_home
                }
            )

            binding.ivMoreOptions.setOnClickListener { anchor ->
                showPopupMenu(anchor, item, onEditClick, onDeleteClick)
            }
        }

        private fun showPopupMenu(anchor: View, item: AddressEntity, onEditClick: (AddressEntity) -> Unit, onDeleteClick: (AddressEntity) -> Unit) {
            val popupView = LayoutInflater.from(anchor.context).inflate(R.layout.item_popup_menu, null)
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            popupView.findViewById<View>(R.id.actionEdit).setOnClickListener {
                popupWindow.dismiss()
                onEditClick(item)
            }

            popupView.findViewById<View>(R.id.actionDelete).setOnClickListener {
                popupWindow.dismiss()
                onDeleteClick(item)
            }

            popupWindow.elevation = 12f
            popupWindow.isOutsideTouchable = true
            popupWindow.showAsDropDown(anchor, -150, 0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSavedAddressProfileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onEditClick, onDeleteClick)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AddressEntity>() {
        override fun areItemsTheSame(oldItem: AddressEntity, newItem: AddressEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AddressEntity, newItem: AddressEntity) = oldItem == newItem
    }
}
