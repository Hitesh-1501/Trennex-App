package com.example.trennex.ui.home.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemSavedAddressBinding


class SaveAddressAdapter(
    private val onItemClick: (SavedAddressItem) -> Unit,
    private val onMoreClick: (View, SavedAddressItem) -> Unit
): RecyclerView.Adapter<SaveAddressAdapter.AddressViewHolder>(){

    data class SavedAddressItem(
        val userName: String,
        val address: String
    )
    private var addresses: List<SavedAddressItem> = emptyList()
    private var selectedAddress: String? = null

    private var currentUserName: String = "Guest User"

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(items: List<SavedAddressItem>, selected: String?, title: String ){
        addresses = items
        selectedAddress = selected
        currentUserName = title
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddressViewHolder {
        return AddressViewHolder(
            ItemSavedAddressBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: AddressViewHolder,
        position: Int
    ) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int = addresses.size

    inner class AddressViewHolder(
        private val binding: ItemSavedAddressBinding
    ): RecyclerView.ViewHolder(binding.root){
        fun bind(item: SavedAddressItem){
            binding.savedAddressTitle.text = currentUserName
            binding.savedAddressText.text = item.address
            binding.selectedBadge.isVisible = item.address == selectedAddress
            binding.root.setOnClickListener { onItemClick(item) }
            binding.moreAddressOptions.setOnClickListener { onMoreClick(it, item) }
        }
    }
}