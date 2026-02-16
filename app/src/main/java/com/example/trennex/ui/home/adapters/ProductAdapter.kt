package com.example.trennex.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemProductBinding
import com.example.trennex.ui.home.model.ProductModel

class ProductAdapter(
    private val products: List<ProductModel>,
    private val onProductClick : () -> Unit
): RecyclerView.Adapter<ProductAdapter.ProductVieewHolder>(){
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
       holder.binding.ivProduct.setImageResource(item.productImage)
       holder.binding.tvProductTitle.text = item.productName
       holder.binding.tvProductPrice.text = "₹${item.productPrice}"

       holder.itemView.setOnClickListener {
           onProductClick()
       }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    inner class ProductVieewHolder(val binding: ItemProductBinding): RecyclerView.ViewHolder(binding.root)

}