package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemFashionCategoryBinding
import com.example.trennex.ui.category.model.FashionCategoryModel

class FashionCategoryAdapter(
    private val items : List<FashionCategoryModel>
): RecyclerView.Adapter<FashionCategoryAdapter.ViewHolder>() {

    inner class ViewHolder (val binding: ItemFashionCategoryBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FashionCategoryAdapter.ViewHolder {
        val binding = ItemFashionCategoryBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)

    }

    override fun onBindViewHolder(holder: FashionCategoryAdapter.ViewHolder, position: Int) {
        val items = items[position]
        holder.binding.ivFashion.setImageResource(items.image)
        holder.binding.tvFashion.text = items.name
    }

    override fun getItemCount(): Int {
       return items.size
    }

}