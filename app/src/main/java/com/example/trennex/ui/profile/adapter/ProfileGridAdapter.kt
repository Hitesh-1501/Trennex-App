package com.example.trennex.ui.profile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.databinding.ItemProfileGridBinding
import com.example.trennex.ui.profile.model.ProfileGridItem

class ProfileGridAdapter(
    private val items: List<ProfileGridItem>,
    private val onItemClick: (ProfileGridItem) -> Unit
): RecyclerView.Adapter<ProfileGridAdapter.GridViewHolder>() {

    class GridViewHolder(val binding: ItemProfileGridBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {

        val binding = ItemProfileGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {

        val item = items[position]

        holder.binding.ivGridIcon.setImageResource(item.icon)
        holder.binding.tvGridTitle.text = item.title
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}