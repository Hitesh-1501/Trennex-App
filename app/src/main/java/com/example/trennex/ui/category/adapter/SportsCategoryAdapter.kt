package com.example.trennex.ui.category.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.trennex.R
import com.example.trennex.ui.category.model.FurnitureCategoryModel
import com.example.trennex.ui.category.model.SportsCategoryModel
import com.example.trennex.ui.category.model.ToysCategoryModel

class SportsCategoryAdapter(
    private val items : List<SportsCategoryModel>
): RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    companion object{
        const val TYPE_SPORTS_BANNER = 0
        const val TYPE_SPORTS_HEADER = 1
        const val TYPE_SPORTS_ITEM = 2
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].Type
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when(viewType){
            TYPE_SPORTS_BANNER -> {
                val view = inflater.inflate(R.layout.item_sports_banner,parent,false)
                return BannerVH(view)
            }
            TYPE_SPORTS_HEADER -> {
                val view = inflater.inflate(R.layout.item_sports_header,parent,false)
                return HeaderVH(view)
            }
            else  ->{
                val view = inflater.inflate(R.layout.item_sports_category,parent,false)
                return ItemVH(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = items[position]
        when(holder){
            is HeaderVH ->{
                holder.tvHeader.text = item.title
            }
            is ItemVH ->{
                holder.tvName.text = item.title
                holder.ivImage.setImageResource(item.image!!)
            }
        }
    }

    override fun getItemCount(): Int {
       return items.size
    }

    class BannerVH(view: View): RecyclerView.ViewHolder(view)
    class HeaderVH(view: View): RecyclerView.ViewHolder(view){
        val tvHeader : TextView = view.findViewById(R.id.tvSportsHeader)
    }
    class ItemVH(view: View):RecyclerView.ViewHolder(view){
        val tvName:TextView = view.findViewById(R.id.tv_sports)
        val ivImage: ImageView = view.findViewById(R.id.iv_sports)
    }
}