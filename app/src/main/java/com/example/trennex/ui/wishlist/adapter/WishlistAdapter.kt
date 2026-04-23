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
): RecyclerView.Adapter<WishlistAdapter.WishlistVH>(){

    inner class WishlistVH(val binding: ItemWishlistProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistVH {
        val binding = ItemWishlistProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistVH(binding)
    }

    override fun onBindViewHolder(holder: WishlistAdapter.WishlistVH, position: Int) {
        val item = items[position]
        holder.binding.tvTitle.text = item.title
        holder.binding.tvPrice.text = CurrencyFormator.formatInr(item.price)
        Glide.with(holder.binding.ivProduct)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.binding.ivProduct)
    }

    override fun getItemCount(): Int {
       return items.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(updateItems: List<WishlistItemsModel>){
        items = updateItems
        notifyDataSetChanged()
    }
}