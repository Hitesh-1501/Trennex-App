package com.example.trennex.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemCategoryBinding
import com.example.trennex.ui.home.model.CategoryModel

class CategoryAdapter(
 private val list : List<CategoryModel>,
    private val onClick : (CategoryModel) -> Unit
): RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>(){
    private var selectedPosition = 0
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        val item = list[position]
        holder.binding.categoryImg.setImageResource(item.icon)
        holder.binding.categoryTitle.text = item.title
        val isSelected = (position == selectedPosition)
        holder.binding.categoryLayout.isSelected = isSelected
        holder.binding.categoryTitle.isSelected = isSelected
        holder.binding.categoryImg.isSelected = isSelected
        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if(selectedPosition != currentPosition){
                val previousPosition = selectedPosition
                selectedPosition = currentPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onClick(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class CategoryViewHolder(val binding: ItemCategoryBinding): RecyclerView.ViewHolder(binding.root)
}