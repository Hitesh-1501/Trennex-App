package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemMobileCategoryBinding
import com.example.trennex.ui.category.model.MobileCategoryModel

class MobileCategoryAdapter(
    private val mobileCategoryList: List<MobileCategoryModel>
): RecyclerView.Adapter<MobileCategoryAdapter.MobileCategoryViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MobileCategoryViewHolder {
       val binding = ItemMobileCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MobileCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MobileCategoryViewHolder,
        position: Int
    ) {
       val item = mobileCategoryList[position]
       holder.binding.ivMobiles.setImageResource(item.image)
    }

    override fun getItemCount(): Int {
       return mobileCategoryList.size
    }

    inner class MobileCategoryViewHolder(val binding: ItemMobileCategoryBinding): RecyclerView.ViewHolder(binding.root)

}