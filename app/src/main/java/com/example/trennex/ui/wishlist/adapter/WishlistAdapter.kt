package com.example.trennex.ui.wishlist.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemWishlistProductBinding
import com.example.trennex.ui.wishlist.model.WishlistItemsModel
import com.example.trennex.utils.CurrencyFormator
import androidx.core.graphics.toColorInt

class WishlistAdapter(
    private var items : List<WishlistItemsModel>,
    private val onItemClicked: (WishlistItemsModel) -> Unit,
    private val onAddToCartClicked: (WishlistItemsModel) -> Unit,
    private val onRemoveClicked: (WishlistItemsModel) -> Unit,
    private val onSelectionChanged: (Int) -> Unit,
    private val onItemSelectionToggled: (Int) -> Unit
): RecyclerView.Adapter<WishlistAdapter.WishlistVH>(){

    private var isSelectionMode = false
    private var selectedIds = emptySet<Int>()
    
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
        
        holder.binding.root.setCardBackgroundColor(
            if(isSelectionMode && isSelected) "#E8F0FF".toColorInt() else Color.WHITE
        )
        
        holder.binding.root.setOnClickListener {
            if(isSelectionMode){
                onItemSelectionToggled(item.id)
            }else{
                onItemClicked(item)
            }
        }
        holder.binding.btnAddToCart.setOnClickListener {
            if (!isSelectionMode) {
                onAddToCartClicked(item)
            } else {
                onItemSelectionToggled(item.id)
            }
        }
        holder.binding.ivRemove.setOnClickListener {
            if (isSelectionMode) {
                onItemSelectionToggled(item.id)
            } else {
                onRemoveClicked(item)
            }
        }

        holder.binding.ivRemove.setImageResource(
            if(isSelectionMode){
                if(isSelected) R.drawable.wishlist_checkbox_check else R.drawable.wishlist_checkbox_unchecked
            }else{
                R.drawable.baseline_close_24
            }
        )
        holder.binding.ivRemove.background = if(isSelectionMode) null else holder.binding.root.context.getDrawable(R.drawable.bg_circle_grey)
        holder.binding.ivRemove.imageTintList = if (isSelectionMode) null else holder.binding.ivRemove.context.getColorStateList(R.color.textPrimary)
        
        val padding = if (isSelectionMode) 0 else 6
        holder.binding.ivRemove.setPadding(padding, padding, padding, padding)
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(updateItems: List<WishlistItemsModel>){
        items = updateItems
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionMode(enabled: Boolean){
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedIds(ids: Set<Int>) {
        if (selectedIds != ids) {
            selectedIds = ids
            onSelectionChanged(selectedIds.size)
            notifyDataSetChanged()
        }
    }

    fun isSelectionMode(): Boolean = isSelectionMode

    fun getSelectedIds(): Set<Int> = selectedIds
}