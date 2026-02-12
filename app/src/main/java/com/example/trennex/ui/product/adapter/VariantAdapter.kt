package com.example.trennex.ui.product.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemVariantBinding
import com.example.trennex.ui.product.model.VariantModel

class VariantAdapter(
    private val variants : List<VariantModel>,
    private val onVariantName : (String) -> Unit,
    private val onVariantSelected : (VariantModel) -> Unit
): RecyclerView.Adapter<VariantAdapter.VariantViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VariantViewHolder {
        val binding = ItemVariantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VariantViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(
        holder: VariantViewHolder,
        position: Int
    ) {
        val variant = variants[position]
        holder.binding.tvStorage.text = variant.variant
        holder.binding.tvPrice.text = variant.Price

        if(variant.isSelected){
            holder.binding.variantLayout.setBackgroundResource(R.drawable.variant_selected)
            holder.binding.tvStorage.setTextColor(Color.WHITE)
            holder.binding.tvPrice.setTextColor(Color.WHITE)
            holder.binding.tvStorage.setTypeface(holder.binding.tvStorage.typeface, Typeface.BOLD)
            holder.binding.tvPrice.setTypeface(holder.binding.tvPrice.typeface, Typeface.BOLD)
        }else{
            holder.binding.variantLayout.setBackgroundResource(R.drawable.variant_unselected)
            holder.binding.tvStorage.setTextColor(Color.BLACK)
            holder.binding.tvPrice.setTextColor(Color.BLACK)
            holder.binding.tvStorage.setTypeface(holder.binding.tvStorage.typeface, Typeface.NORMAL)
            holder.binding.tvPrice.setTypeface(holder.binding.tvPrice.typeface, Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener {
            variants.forEach { it.isSelected = false }
            variant.isSelected = true
            notifyDataSetChanged()
            onVariantSelected(variant)
            onVariantName(variant.variant)
        }
    }

    override fun getItemCount(): Int {
       return variants.size
    }

    inner class VariantViewHolder(val binding : ItemVariantBinding) : RecyclerView.ViewHolder(binding.root)


}