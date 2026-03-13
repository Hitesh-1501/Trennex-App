package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemCategoryTabBinding
import com.example.trennex.ui.category.model.CategoryModel

class CategorySideBarAdapter(
    private val categories : List<CategoryModel>,
    private val onCategoryClick: (CategoryModel) -> Unit
): RecyclerView.Adapter<CategorySideBarAdapter.ViewHolder>(){
    private var selectedPosition = 0
    inner class ViewHolder(val binding: ItemCategoryTabBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(category : CategoryModel, position: Int) {
            binding.ivCategoryIcon.setImageResource(category.icon)
            binding.tvCategoryName.text = category.name
            if(selectedPosition == position){
                binding.activeIndicatorView.visibility = ViewGroup.VISIBLE
                binding.tvCategoryName.typeface = ResourcesCompat.getFont(binding.root.context, R.font.poppins_semibold)
                binding.categoryTabContainer.setBackgroundResource(R.drawable.tab_active)
            }else{
                binding.categoryTabContainer.setBackgroundResource(R.drawable.tab_inactive)
                binding.tvCategoryName.typeface = ResourcesCompat.getFont(binding.root.context, R.font.poppins_regular)
                binding.activeIndicatorView.visibility = ViewGroup.GONE
            }
            binding.root.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onCategoryClick(category)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategorySideBarAdapter.ViewHolder {
        val binding = ItemCategoryTabBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategorySideBarAdapter.ViewHolder, position: Int) {
        holder.bind(categories[position],position)
    }

    override fun getItemCount(): Int {
        return categories.size
    }
}