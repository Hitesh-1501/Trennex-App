package com.example.trennex.ui.home.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemSavedAddressBinding


class SaveAddressAdapter(
    private val onItemClick: (SavedAddressItem) -> Unit,
    private val onMoreClick: (View, SavedAddressItem) -> Unit
): RecyclerView.Adapter<SaveAddressAdapter.AddressViewHolder>(){

    data class SavedAddressItem(
        val id: String = "",
        val userName: String,
        val flatNo: String = "",
        val address: String,
        val mobile: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val placeName: String = "",
        val addressType: String = ADDRESS_TYPE_HOME
    ) {
        val displayAddress: String
            get() = listOf(flatNo, address)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
    }

    private var addresses: List<SavedAddressItem> = emptyList()
    private var selectedAddressId: String? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(items: List<SavedAddressItem>, selectedId: String? ){
        addresses = items
        selectedAddressId = selectedId
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
            binding.homeIcon.setImageResource(
                if (item.addressType.equals(ADDRESS_TYPE_OFFICE, ignoreCase = true)){
                    R.drawable.ic_office
                } else {
                    R.drawable.ic_home
                }
            )
            binding.savedAddressTitle.text = item.userName
            binding.savedAddressText.text = item.displayAddress
            binding.selectedBadge.isVisible = item.id.isNotBlank() && item.id == selectedAddressId
            binding.root.setOnClickListener { onItemClick(item) }
            binding.moreAddressOptions.setOnClickListener { onMoreClick(it, item) }
        }
    }

    companion object {
        const val ADDRESS_TYPE_HOME = "Home"
        const val ADDRESS_TYPE_OFFICE = "Office"
    }
}