package com.example.trennex.ui.product.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemProductBannerBinding

class ProductImageAdapter(
    private val images: List<Int>,
    private val onImgClick : (ImageView, Int) -> Unit
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
        ViewCompat.setTransitionName(holder.binding.imgProducts,null)
        holder.binding.imgProducts.setImageResource(item)
        holder.itemView.setOnClickListener {
            ViewCompat.setTransitionName(holder.binding.imgProducts,"product_image")
            onImgClick(holder.binding.imgProducts,position)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ProductImageViewHolder(val binding: ItemProductBannerBinding): RecyclerView.ViewHolder(binding.root)


}