package com.example.trennex.ui.product.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemColorVariantBinding
import com.example.trennex.ui.product.model.ProductColorModel

class ColorVariantAdapter(
    private val colorVariantList : List<ProductColorModel>,
    private val onVariantName : (String) -> Unit,
    private val onVariantSelected: (ProductColorModel) -> Unit
): RecyclerView.Adapter<ColorVariantAdapter.ColorVariantViewHolder>(){

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
        holder.binding.colorVariants.setImageResource(item.img)
        if(item.isSelected){
            holder.binding.productLayout.setBackgroundResource(R.drawable.bg_product_color_selected)
        }else{
            holder.binding.productLayout.setBackgroundResource(R.drawable.bg_product_color_unselected)
        }
        holder.binding.productLayout.setOnClickListener {
            colorVariantList.forEach { it.isSelected = false }
            item.isSelected = true
            notifyDataSetChanged()
            onVariantSelected(item)
            onVariantName(item.modelName)
        }
    }

    override fun getItemCount(): Int {
        return colorVariantList.size
    }

    inner class ColorVariantViewHolder(val binding: ItemColorVariantBinding): RecyclerView.ViewHolder(binding.root)
}