package com.example.trennex.ui.checkout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemCheckoutProductBinding
import com.example.trennex.ui.cart.model.CartItemModel

class CheckoutProductAdapter : ListAdapter<CartItemModel, CheckoutProductAdapter.ProductVH>(DiffCallback) {

    class ProductVH(val binding: ItemCheckoutProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductVH {
        val binding = ItemCheckoutProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductVH(binding)
    }

    override fun onBindViewHolder(holder: ProductVH, position: Int) {
        val item = getItem(position)
        Glide.with(holder.binding.ivProduct.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.binding.ivProduct)
        
        holder.binding.tvDeliveryDate.text = "Estimate delivery date: ${item.deliveryDetails.ifBlank { "Sunday, 11 January" }}"
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CartItemModel>() {
        override fun areItemsTheSame(oldItem: CartItemModel, newItem: CartItemModel): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CartItemModel, newItem: CartItemModel): Boolean = oldItem == newItem
    }
}