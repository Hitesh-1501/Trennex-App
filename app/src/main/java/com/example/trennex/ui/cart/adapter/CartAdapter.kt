package com.example.trennex.ui.cart.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemCartBinding
import com.example.trennex.ui.cart.model.CartItemModel

class CartAdapter(
    private val cartItems: List<CartItemModel>

): RecyclerView.Adapter<CartAdapter.CartViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CartViewHolder,
        position: Int
    ) {
        val item = cartItems[position]
        holder.binding.imgProduct.setImageResource(item.image)
        holder.binding.tvTitle.text = item.title
        holder.binding.tvDescription.text = item.description
        holder.binding.tvMrp.paintFlags = holder.binding.tvMrp.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        holder.binding.tvMrp.text = "₹${item.mrp}"
        holder.binding.tvPrice.text = "₹${item.Price}"
        holder.binding.tvQty.text = "Qty: ${item.quantity}"
    }

    override fun getItemCount(): Int {
       return cartItems.size
    }

    inner class  CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)


}