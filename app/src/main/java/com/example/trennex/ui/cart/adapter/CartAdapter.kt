package com.example.trennex.ui.cart.adapter

import android.annotation.SuppressLint
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemCartBinding
import com.example.trennex.ui.cart.model.CartItemModel
import com.example.trennex.utils.CurrencyFormator
import kotlin.math.roundToInt

class CartAdapter(
    private val onItemSelectionChanged:(itemId: Int,selected: Boolean) -> Unit,
    private val onQuantitySelected:(itemId: Int,quantity: Int) -> Unit
): RecyclerView.Adapter<CartAdapter.CartViewHolder>(){

    private val cartItems = mutableListOf<CartItemModel>()


    @SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<CartItemModel>){
        cartItems.clear()
        cartItems.addAll(items)
        notifyDataSetChanged()
    }


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
        with(holder.binding){
            if(!item.imageUrl.isNullOrBlank()){
                Glide.with(imgProduct)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(item.imageRes ?: R.drawable.placeholder)
                    .into(imgProduct)
            }else{
                imgProduct.setImageResource(item.imageRes ?: R.drawable.placeholder)
            }
            tvTitle.text = item.title
            tvDescription.text = item.description
            tvRating.text = "${buildStars(item.rating)} ${String.format("%.1f", item.rating)} (${item.ratingCount})"
            tvMrp.paintFlags = tvMrp.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tvMrp.text = CurrencyFormator.formatInr(item.mrp)
            tvPrice.text = CurrencyFormator.formatInr(item.price)
            tvReturnPolicy.text = item.returnPolicy.ifBlank { "10 days return available" }
            tvDeliveryDetails.text = item.deliveryDetails.ifBlank { "Delivery details unavailable" }
            tvQty.text = "Qty: ${item.quantity} ▼"

            checkItem.setOnCheckedChangeListener(null)
            checkItem.isChecked = item.isSelected
            checkItem.setOnCheckedChangeListener { _,isChecked ->
                onItemSelectionChanged(item.id,isChecked)
            }

            tvQty.setOnClickListener {
                val popupMenu = PopupMenu(it.context,it)
                (1..5).forEach { qty->
                    popupMenu.menu.add(0,qty,qty,"Qty: $qty")
                }
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    onQuantitySelected(item.id,menuItem.itemId)
                    true
                }
                popupMenu.show()
            }
        }
    }

    override fun getItemCount(): Int {
       return cartItems.size
    }

    private fun buildStars(rating: Double): String{
        val clamped = rating.coerceIn(0.0,5.0)
        val fullStars = clamped.roundToInt().coerceIn(0,5)
        return "★".repeat(fullStars) + "☆".repeat(5 - fullStars)
    }

    inner class  CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)


}