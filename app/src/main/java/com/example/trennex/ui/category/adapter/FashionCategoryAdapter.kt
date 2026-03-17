package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.databinding.ItemFashionCategoryBinding
import com.example.trennex.databinding.ItemFashionHeaderBinding
import com.example.trennex.ui.category.model.FashionCategoryModel

class FashionCategoryAdapter(
    private val items : List<FashionCategoryModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType){
            TYPE_BANNER ->{
                val view = inflater.inflate(R.layout.item_fashion_banner,parent,false)
                BannerVH(view)
            }
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_fashion_header,parent,false)
                HeaderVH(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_fashion_category,parent,false)
                ItemVH(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = items[position]
        when(holder){
            is HeaderVH -> {
                holder.tvHeader.text = item.name
            }
            is ItemVH -> {
                holder.tvName.text = item.name
                holder.ivImage.setImageResource(item.image!!)
            }
        }
    }

    override fun getItemCount() = items.size


    companion object {
        const val TYPE_BANNER = 0
        const val TYPE_HEADER = 1
        const val TYPE_ITEM = 2
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].Type
    }

    class BannerVH(view: View): RecyclerView.ViewHolder(view)

    class HeaderVH(view: View): RecyclerView.ViewHolder(view){
        val tvHeader: TextView = view.findViewById(R.id.tvHeader)
    }
    class ItemVH(view: View):RecyclerView.ViewHolder(view){
        val tvName:TextView = view.findViewById(R.id.tv_fashion)
        val ivImage: ImageView = view.findViewById(R.id.iv_fashion)
    }
}