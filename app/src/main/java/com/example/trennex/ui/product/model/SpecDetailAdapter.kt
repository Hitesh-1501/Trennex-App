package com.example.trennex.ui.product.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemSpecDetailBinding

class SpecDetailAdapter(): RecyclerView.Adapter<SpecDetailAdapter.SpecViewHolder>(){

    private var items = mutableListOf<SpecDetailItem>()

    inner class SpecViewHolder(val binding: ItemSpecDetailBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SpecDetailAdapter.SpecViewHolder {
        val binding = ItemSpecDetailBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SpecViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SpecDetailAdapter.SpecViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvSpecLabel.text = item.label
        holder.binding.tvSpecValue.text = item.value

    }

    override fun getItemCount() : Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<SpecDetailItem>){
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()

    }

}