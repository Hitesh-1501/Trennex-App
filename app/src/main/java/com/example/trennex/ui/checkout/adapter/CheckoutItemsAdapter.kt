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

class CheckoutItemsAdapter : ListAdapter<CartItemModel, CheckoutItemsAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemCheckoutProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItemModel) {
            if (item.imageUrl != null) {
                Glide.with(binding.ivProduct).load(item.imageUrl).placeholder(R.drawable.placeholder).into(binding.ivProduct)
            } else if (item.imageRes != null) {
                binding.ivProduct.setImageResource(item.imageRes)
            }
            
            // For now, static delivery estimate as in the design
            binding.tvDeliveryEstimate.text = "Estimate delivery date: Sunday, 11 January"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCheckoutProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CartItemModel>() {
        override fun areItemsTheSame(oldItem: CartItemModel, newItem: CartItemModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CartItemModel, newItem: CartItemModel) = oldItem == newItem
    }
}
