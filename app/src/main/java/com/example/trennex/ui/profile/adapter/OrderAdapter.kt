package com.example.trennex.ui.profile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemOrderBinding
import com.example.trennex.ui.profile.model.OrderModel
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter : ListAdapter<OrderModel, OrderAdapter.OrderViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

    class OrderViewHolder(private val binding: ItemOrderBinding, private val dateFormat: SimpleDateFormat) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: OrderModel) {
            if (order.imageUrl != null) {
                Glide.with(binding.ivProduct).load(order.imageUrl).placeholder(R.drawable.placeholder).into(binding.ivProduct)
            } else if (order.imageRes != null) {
                binding.ivProduct.setImageResource(order.imageRes)
            }
            
            binding.tvTitle.text = order.title
            
            if (order.status == "DELIVERED") {
                binding.tvStatus.text = "Delivered on"
                binding.tvDate.text = dateFormat.format(order.expectedDeliveryDate.toDate())
                binding.layoutRating.visibility = View.VISIBLE
            } else {
                binding.tvStatus.text = "Your order is expected to be delivered by"
                binding.tvDate.text = dateFormat.format(order.expectedDeliveryDate.toDate())
                binding.layoutRating.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        return OrderViewHolder(
            ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            dateFormat
        )
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<OrderModel>() {
        override fun areItemsTheSame(oldItem: OrderModel, newItem: OrderModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OrderModel, newItem: OrderModel) = oldItem == newItem
    }
}
