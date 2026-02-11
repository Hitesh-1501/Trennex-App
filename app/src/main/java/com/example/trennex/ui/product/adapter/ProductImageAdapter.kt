package com.example.trennex.ui.product.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemProductBannerBinding

class ProductImageAdapter(
    private val images: List<Int>
): RecyclerView.Adapter<ProductImageAdapter.ProductImageViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductImageViewHolder {
        val binding = ItemProductBannerBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ProductImageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ProductImageViewHolder,
        position: Int
    ) {
        val item = images[position]
        holder.binding.imgProducts.setImageResource(item)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ProductImageViewHolder(val binding: ItemProductBannerBinding): RecyclerView.ViewHolder(binding.root)


}