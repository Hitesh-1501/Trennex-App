package com.example.trennex.ui.wishlist.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemWishlistProductBinding
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.example.trennex.utils.CurrencyFormator

class WishlistAdapter(
    private var items : List<WishlistItemsModel>,
    private val onItemClicked: (WishlistItemsModel) -> Unit,
    private val onAddToCartClicked: (WishlistItemsModel) -> Unit,
    private val onRemoveClicked: (WishlistItemsModel) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
): RecyclerView.Adapter<WishlistAdapter.WishlistVH>(){

    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<Int>()
    inner class WishlistVH(val binding: ItemWishlistProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistVH {
        val binding = ItemWishlistProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistVH(binding)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: WishlistAdapter.WishlistVH, position: Int) {
        val item = items[position]
        val isSelected = selectedIds.contains(item.id)
        holder.binding.tvTitle.text = item.title
        holder.binding.tvPrice.text = CurrencyFormator.formatInr(item.price)
        Glide.with(holder.binding.ivProduct)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.binding.ivProduct)
        holder.binding.root.alpha = if(isSelectionMode && isSelected) 0.7f else 1f
        holder.binding.root.setOnClickListener {
            if(isSelectionMode){
                toggleItemSelection(item.id)
            }else{
                onItemClicked(item)
            }
        }
        holder.binding.btnAddToCart.setOnClickListener {
            if (!isSelectionMode) {
                onAddToCartClicked(item)
            } else {
                toggleItemSelection(item.id)
            }
        }
        holder.binding.ivRemove.setOnClickListener {
            if (isSelectionMode) {
                toggleItemSelection(item.id)
            } else {
                onRemoveClicked(item)
            }
        }

        holder.binding.ivRemove.setImageResource(
            if(isSelectionMode){
                if(isSelected) R.drawable.bg_cart_checkbox_check else R.drawable.bg_cart_unchecked
            }else{
                R.drawable.baseline_close_24
            }
        )
        holder.binding.ivRemove.background = if(isSelectionMode) null else holder.binding.root.context.getDrawable(R.drawable.bg_circle_grey)
        holder.binding.ivRemove.setPadding(
            if (isSelectionMode) 0 else 6,
            if (isSelectionMode) 0 else 6,
            if (isSelectionMode) 0 else 6,
            if (isSelectionMode) 0 else 6
        )
    }

    override fun getItemCount(): Int {
       return items.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(updateItems: List<WishlistItemsModel>){
        items = updateItems
        selectedIds.retainAll ( items.map { it.id }.toSet())
        onSelectionChanged(selectedIds.size)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionMode(enabled: Boolean){
        isSelectionMode = enabled
        if(!enabled){
            selectedIds.clear()
            onSelectionChanged(0)
        }
        notifyDataSetChanged()
    }

    fun isSelectionMode(): Boolean = isSelectionMode

    fun getSelectedItems(): List<WishlistItemsModel> = items.filter { selectedIds.contains(it.id) }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection(){
        selectedIds.clear()
        onSelectionChanged(0)
        notifyDataSetChanged()
    }
    private fun toggleItemSelection(itemId: Int){
        if(selectedIds.contains(itemId)){
            selectedIds.remove(itemId)
        }else{
            selectedIds.add(itemId)
        }
        onSelectionChanged(selectedIds.size)
        notifyItemChanged(items.indexOfFirst { it.id == itemId })
    }
}
