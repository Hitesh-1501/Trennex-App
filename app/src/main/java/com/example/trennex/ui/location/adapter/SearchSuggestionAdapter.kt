package com.example.trennex.ui.location.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemSearchSuggestionBinding
import com.google.android.libraries.places.api.model.AutocompletePrediction

class SearchSuggestionAdapter(
    private val onItemClick: (AutocompletePrediction) -> Unit
): RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder>(){
    private val suggestions = mutableListOf<AutocompletePrediction>()


    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<AutocompletePrediction>){
        suggestions.clear()
        suggestions.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemSearchSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int {
        return suggestions.size
    }

    inner class ViewHolder(
        private val binding: ItemSearchSuggestionBinding
    ): RecyclerView.ViewHolder(binding.root){
        fun bind(item: AutocompletePrediction){
            binding.placeNameTv.text = item.getFullText(null).toString()
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}