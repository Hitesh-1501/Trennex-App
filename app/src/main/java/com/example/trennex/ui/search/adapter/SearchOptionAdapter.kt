package com.example.trennex.ui.search.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemSearchOptionsBinding

class SearchOptionAdapter(
    private val onItemClick:(String,Boolean) -> Unit,
    private val onRemoveClick: (String) -> Unit
): RecyclerView.Adapter<SearchOptionAdapter.SearchOptionViewHolder>() {

    private val items = mutableListOf<String>()
    private var isRecommendation: Boolean = false

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(items: List<String>, isRecommendation: Boolean) {
        this.items.clear()
        this.items.addAll(items)
        this.isRecommendation = isRecommendation
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchOptionViewHolder {
        val binding =
            ItemSearchOptionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchOptionViewHolder(binding = binding)
    }

    override fun onBindViewHolder(
        holder: SearchOptionViewHolder,
        position: Int
    ) {
        holder.bind(items[position], isRecommendation)
    }

    override fun getItemCount(): Int {
        return items.size
    }
    inner class SearchOptionViewHolder(
        private val binding: ItemSearchOptionsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String, isRecommendation: Boolean) {
            binding.title.text = text
            binding.leftIcon.setImageResource(if (isRecommendation) R.drawable.search_mini_icon else R.drawable.history_icon)
            binding.actionIcon.setImageResource(if (isRecommendation) R.drawable.upper_icon else R.drawable.baseline_close_24)
            
            binding.actionIcon.setOnClickListener {
                if(!isRecommendation){
                    onRemoveClick(text)
                }else{
                    onItemClick(text, isRecommendation)
                }
            }
            binding.itemContentLayout.setOnClickListener {
                onItemClick(text, isRecommendation)
            }
        }
    }
}