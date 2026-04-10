package com.example.trennex.ui.product.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trennex.R
import com.example.trennex.databinding.ItemColorVariantBinding
import com.example.trennex.ui.product.model.ProductColorModel

class ColorVariantAdapter(
    private val colorVariantList : List<ProductColorModel>,
    private val onVariantName : (String) -> Unit,
    private val onVariantSelected: (ProductColorModel,Int) -> Unit
): RecyclerView.Adapter<ColorVariantAdapter.ColorVariantViewHolder>(){

    private var selectedPosition = 0

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedColorPosition(position: Int){
        selectedPosition = position
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ColorVariantViewHolder {
        val binding = ItemColorVariantBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ColorVariantViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(
        holder: ColorVariantViewHolder,
        position: Int
    ) {
        val item = colorVariantList[position]
        Glide.with(holder.binding.colorVariants.context)
            .load(item.imageUrl)
            .into(holder.binding.colorVariants)
        if(position == selectedPosition){
            holder.binding.productLayout.setBackgroundResource(R.drawable.bg_product_color_selected)
        }else{
            holder.binding.productLayout.setBackgroundResource(R.drawable.bg_product_color_unselected)
        }
        holder.binding.productLayout.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onVariantSelected(item,selectedPosition)
            onVariantName(item.modelName)
        }
    }

    override fun getItemCount(): Int {
        return colorVariantList.size
    }

    inner class ColorVariantViewHolder(val binding: ItemColorVariantBinding): RecyclerView.ViewHolder(binding.root)
}