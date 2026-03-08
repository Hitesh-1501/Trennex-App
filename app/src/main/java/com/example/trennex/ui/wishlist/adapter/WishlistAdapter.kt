package com.example.trennex.ui.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemWishlistProductBinding
import com.example.trennex.ui.wishlist.model.WishlistItemsModel

class WishlistAdapter(
    private val items : List<WishlistItemsModel>
): RecyclerView.Adapter<WishlistAdapter.WishlistVH>(){

    inner class WishlistVH(val binding: ItemWishlistProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WishlistAdapter.WishlistVH {
        val binding = ItemWishlistProductBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return WishlistVH(binding)
    }

    override fun onBindViewHolder(holder: WishlistAdapter.WishlistVH, position: Int) {
        val items = items[position]
        holder.binding.ivProduct.setImageResource(items.image)
        holder.binding.tvTitle.text = items.title
        holder.binding.tvPrice.text = "₹${items.price}"
    }

    override fun getItemCount(): Int {
       return items.size
    }
}