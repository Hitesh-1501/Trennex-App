package com.example.trennex.ui.product.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.databinding.ItemProductBannerBinding

class ProductImageAdapter(
    private var images: List<String>,
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
        Glide.with(holder.itemView.context)
            .load(item)
            .into(holder.binding.imgProducts)
        holder.itemView.setOnClickListener {
            ViewCompat.setTransitionName(holder.binding.imgProducts,"product_image")
            onImgClick(holder.binding.imgProducts,position)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateImages(newImages : List<String>){
        images  = newImages
        notifyDataSetChanged()
    }
    inner class ProductImageViewHolder(val binding: ItemProductBannerBinding): RecyclerView.ViewHolder(binding.root)


}