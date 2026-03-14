package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemTopPicksBinding
import com.example.trennex.ui.category.model.SubCategoryModel

class TopPicksCategoryAdapter(
    private val topPicksCategory : List<SubCategoryModel>
): RecyclerView.Adapter<TopPicksCategoryAdapter.ViewHolder>(){

    inner class ViewHolder(val binding: ItemTopPicksBinding) : RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TopPicksCategoryAdapter.ViewHolder {
        val binding = ItemTopPicksBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopPicksCategoryAdapter.ViewHolder, position: Int) {
       val items = topPicksCategory[position]
       holder.binding.ivCategory.setImageResource(items.image)
       holder.binding.tvCategoryName.text = items.name
    }

    override fun getItemCount(): Int {
        return topPicksCategory.size
    }
}