package com.example.trennex.ui.home.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.databinding.ItemProductBinding
import com.example.trennex.ui.home.model.ProductModel
import com.example.trennex.utils.CurrencyFormator

class ProductAdapter(
    private var products: List<ProductModel>,
    private val onProductClick : (ProductModel) -> Unit
): RecyclerView.Adapter<ProductAdapter.ProductVieewHolder>(){

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<ProductModel>) {
        if (products == newList) return
        products = newList
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductVieewHolder {
       val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ProductVieewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ProductVieewHolder,
        position: Int
    ) {
        val item = products[position]
        Glide.with(holder.itemView.context)
            .load(item.image)
            .into(holder.binding.ivProduct)
        holder.binding.tvProductTitle.text = item.name
        holder.binding.tvProductPrice.text = CurrencyFormator.formatInr(item.price)

       holder.itemView.setOnClickListener {
           onProductClick(item)
       }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    inner class ProductVieewHolder(val binding: ItemProductBinding): RecyclerView.ViewHolder(binding.root)

}